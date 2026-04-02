package cn.wubo.sql.forge.mcp.service;

import cn.wubo.sql.forge.mcp.model.TreeNode;
import cn.wubo.sql.forge.mcp.model.QueryIntent;
import cn.wubo.sql.forge.mcp.model.TableSelection;
import cn.wubo.sql.forge.mcp.model.TableSelection.ColumnUsage;
import cn.wubo.sql.forge.mcp.model.TableSelection.RelevantColumn;
import cn.wubo.sql.forge.mcp.model.TableSelection.SelectedTable;
import cn.wubo.sql.forge.mcp.model.TableSelection.TableRelation;
import cn.wubo.sql.forge.mcp.model.ConversationContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 智能表选择服务
 * 根据用户查询和数据库元数据，选择最相关的表并分析表关系
 * 
 * 替代方案：不依赖 sql-forge-core，使用 RestClient 调用 API 获取元数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableSelector {

    private final ObjectMapper objectMapper;

    // 中文分词词典（简化版）
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "和", "与", "或", "在", "是", "有", "个", "我", "你", "他",
            "查询", "获取", "查找", "看看", "显示", "所有", "全部", "什么", "哪些",
            "怎么", "如何", "请问", "需要", "想要"
    );

    // 相似度计算权重
    private static final double TABLE_NAME_WEIGHT = 0.4;
    private static final double TABLE_REMARK_WEIGHT = 0.3;
    private static final double COLUMN_MATCH_WEIGHT = 0.3;

    /**
     * 从元数据JSON中选择相关表
     */
    public TableSelection selectTables(String query, String schemaJson, QueryIntent intent, ConversationContext context) {
        try {
            // 解析元数据树
            List<TreeNode<?>> schemaNodes = objectMapper.readValue(
                    schemaJson,
                    new TypeReference<List<TreeNode<?>>>() {}
            );

            // 提取查询关键词
            Set<String> keywords = extractKeywords(query);

            // 获取已确认的表
            Set<String> confirmedTables = context != null ? new HashSet<>(context.getRecentConfirmedTables()) : Set.of();

            // 收集所有表及其信息
            List<TableMatch> tableMatches = collectTables(schemaNodes, keywords);

            // 过滤和排序
            List<TableMatch> filteredMatches = tableMatches.stream()
                    .filter(m -> m.getScore() > 0.1 || confirmedTables.contains(m.getTableName()))
                    .sorted(Comparator.comparing(TableMatch::getScore).reversed())
                    .collect(Collectors.toList());

            // 根据意图决定选择数量
            int tableCount = determineTableCount(intent, filteredMatches);

            // 截取前N个表
            if (filteredMatches.size() > tableCount) {
                filteredMatches = filteredMatches.subList(0, tableCount);
            }

            // 分析表关系
            List<TableRelation> relations = analyzeTableRelations(schemaNodes, filteredMatches, keywords);

            // 构建返回结果
            return buildTableSelection(filteredMatches, relations, keywords);

        } catch (Exception e) {
            log.error("表选择失败", e);
            return TableSelection.builder()
                    .selectedTables(Collections.emptyList())
                    .relations(Collections.emptyList())
                    .totalScore(0.0)
                    .build();
        }
    }

    /**
     * 提取查询关键词
     */
    public Set<String> extractKeywords(String query) {
        Set<String> keywords = new HashSet<>();

        // 移除停用词并分词
        for (String word : query.split("[\\s,，。、；：!?]+")) {
            if (!STOP_WORDS.contains(word) && word.length() > 1) {
                keywords.add(word.toLowerCase());
            }
        }

        // 特殊处理英文单词
        Pattern englishPattern = Pattern.compile("([a-zA-Z_]+)");
        Matcher matcher = englishPattern.matcher(query);
        while (matcher.find()) {
            keywords.add(matcher.group(1).toLowerCase());
        }

        return keywords;
    }

    /**
     * 收集所有表并计算相关性
     */
    private List<TableMatch> collectTables(List<TreeNode<?>> schemaNodes, Set<String> keywords) {
        List<TableMatch> matches = new ArrayList<>();

        // 遍历Schema节点
        for (TreeNode<?> schemaNode : schemaNodes) {
            if (schemaNode.getChildren() != null) {
                for (TreeNode<?> tableTypeNode : schemaNode.getChildren()) {
                    if (tableTypeNode.getChildren() != null) {
                        for (TreeNode<?> tableNode : tableTypeNode.getChildren()) {
                            TableMatch match = calculateTableRelevance(tableNode, keywords);
                            if (match.getScore() > 0) {
                                matches.add(match);
                            }
                        }
                    }
                }
            }
        }

        return matches;
    }

    /**
     * 计算表相关性评分
     */
    private TableMatch calculateTableRelevance(TreeNode<?> tableNode, Set<String> keywords) {
        String tableName = tableNode.getLabel();
        String remarks = "";
        List<RelevantColumn> relevantColumns = new ArrayList<>();

        // 获取表备注
        if (tableNode.getData() != null) {
            try {
                Map<String, Object> data = objectMapper.convertValue(tableNode.getData(), Map.class);
                remarks = String.valueOf(data.getOrDefault("remarks", ""));
            } catch (Exception e) {
                log.debug("解析表数据失败", e);
            }
        }

        double score = 0.0;
        int matchedCount = 0;

        // 1. 表名匹配
        double tableNameScore = calculateTextSimilarity(tableName.toLowerCase(), keywords);
        if (tableNameScore > 0.3) {
            matchedCount++;
        }

        // 2. 表备注匹配
        double remarkScore = calculateTextSimilarity(remarks.toLowerCase(), keywords);
        if (remarkScore > 0.3) {
            matchedCount++;
        }

        // 3. 列匹配
        if (tableNode.getChildren() != null) {
            for (TreeNode<?> columnNode : tableNode.getChildren()) {
                String columnName = columnNode.getLabel();
                double columnScore = calculateTextSimilarity(columnName.toLowerCase(), keywords);
                if (columnScore > 0.3) {
                    matchedCount++;
                    ColumnUsage usage = determineColumnUsage(columnName, keywords);
                    relevantColumns.add(RelevantColumn.builder()
                            .columnName(columnName)
                            .columnType(getColumnType(columnNode))
                            .relevanceScore(columnScore)
                            .usage(usage)
                            .matchedKeyword(findMatchedKeyword(columnName, keywords))
                            .build());
                }
            }
        }

        // 综合评分
        score = tableNameScore * TABLE_NAME_WEIGHT +
                remarkScore * TABLE_REMARK_WEIGHT +
                (matchedCount > 0 ? 0.3 : 0);

        // 如果有关键词匹配，提高评分
        if (matchedCount > 0) {
            score = Math.min(1.0, score + 0.2);
        }

        return TableMatch.builder()
                .tableName(tableName)
                .remarks(remarks)
                .score(Math.min(1.0, score))
                .relevantColumns(relevantColumns)
                .build();
    }

    /**
     * 计算文本相似度
     */
    private double calculateTextSimilarity(String text, Set<String> keywords) {

        if (keywords.isEmpty()) {
            return 0;
        }

        int matchCount = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                matchCount++;
            }
        }

        return keywords.isEmpty() ? 0 : (double) matchCount / keywords.size();
    }

    /**
     * 判断列的用途
     */
    private ColumnUsage determineColumnUsage(String columnName, Set<String> keywords) {
        String lowerCol = columnName.toLowerCase();

        // 根据列名判断用途
        if (lowerCol.contains("id") || lowerCol.contains("key") || lowerCol.contains("fk")) {
            return ColumnUsage.JOIN;
        }
        if (lowerCol.contains("time") || lowerCol.contains("date") || lowerCol.contains("create")) {
            return ColumnUsage.ORDER_BY;
        }
        if (lowerCol.contains("count") || lowerCol.contains("sum") || lowerCol.contains("num")) {
            return ColumnUsage.SELECT;
        }

        return ColumnUsage.WHERE;
    }

    /**
     * 获取列类型
     */
    private String getColumnType(TreeNode<?> columnNode) {
        if (columnNode.getData() != null) {
            try {
                Map<String, Object> data = objectMapper.convertValue(columnNode.getData(), Map.class);
                return String.valueOf(data.getOrDefault("typeName", "VARCHAR"));
            } catch (Exception e) {
                log.debug("解析列类型失败", e);
            }
        }
        return "VARCHAR";
    }

    /**
     * 找到匹配的关键词
     */
    private String findMatchedKeyword(String text, Set<String> keywords) {
        for (String keyword : keywords) {
            if (text.toLowerCase().contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }

    /**
     * 根据意图确定选择的表数量
     */
    private int determineTableCount(QueryIntent intent, List<TableMatch> matches) {
        if (intent == null) {
            return Math.min(3, matches.size());
        }

        return switch (intent.getPrimaryIntent()) {
            case SIMPLE_QUERY, COUNT_QUERY -> Math.min(2, matches.size());
            case JOIN_QUERY, SUBQUERY_QUERY -> Math.min(4, matches.size());
            case UNION_QUERY -> Math.min(5, matches.size());
            default -> Math.min(3, matches.size());
        };
    }

    /**
     * 分析表之间的关系
     */
    private List<TableRelation> analyzeTableRelations(List<TreeNode<?>> schemaNodes,
                                                      List<TableMatch> selectedTables,
                                                      Set<String> keywords) {
        List<TableRelation> relations = new ArrayList<>();

        // 简单的关联推断：基于列名相似度
        for (int i = 0; i < selectedTables.size(); i++) {
            for (int j = i + 1; j < selectedTables.size(); j++) {
                TableMatch table1 = selectedTables.get(i);
                TableMatch table2 = selectedTables.get(j);

                // 检查是否有共同的列名
                Set<String> cols1 = table1.getRelevantColumns().stream()
                        .map(RelevantColumn::getColumnName)
                        .collect(Collectors.toSet());
                Set<String> cols2 = table2.getRelevantColumns().stream()
                        .map(RelevantColumn::getColumnName)
                        .collect(Collectors.toSet());

                // 查找可能的关联列
                String joinColumn1 = findJoinColumn(cols1, cols2);
                String joinColumn2 = findJoinColumn(cols2, cols1);

                if (joinColumn1 != null && joinColumn2 != null) {
                    relations.add(TableRelation.builder()
                            .joinType("INNER JOIN")
                            .leftTable(table1.getTableName())
                            .leftColumn(joinColumn1)
                            .rightTable(table2.getTableName())
                            .rightColumn(joinColumn2)
                            .source("inferred")
                            .build());
                }
            }
        }

        return relations;
    }

    /**
     * 查找可能的关联列
     */
    private String findJoinColumn(Set<String> cols1, Set<String> cols2) {
        for (String col1 : cols1) {
            for (String col2 : cols2) {
                // 主键关联：id = xxx_id
                if (col1.equalsIgnoreCase("id") ||
                        col1.equalsIgnoreCase(col2 + "_id") ||
                        col2.equalsIgnoreCase(col1 + "_id")) {
                    return col1;
                }
            }
        }
        return null;
    }

    /**
     * 构建表选择结果
     */
    private TableSelection buildTableSelection(List<TableMatch> matches,
                                                List<TableRelation> relations,
                                                Set<String> keywords) {
        List<SelectedTable> selectedTables = matches.stream()
                .map(m -> SelectedTable.builder()
                        .tableName(m.getTableName())
                        .remarks(m.getRemarks())
                        .relevanceScore(m.getScore())
                        .relevantColumns(m.getRelevantColumns())
                        .fuzzyMatched(m.getScore() < 0.5)
                        .matchReason(buildMatchReason(m, keywords))
                        .build())
                .collect(Collectors.toList());

        double totalScore = matches.stream()
                .mapToDouble(TableMatch::getScore)
                .average()
                .orElse(0.0);

        return TableSelection.builder()
                .selectedTables(selectedTables)
                .relations(relations)
                .totalScore(totalScore)
                .build();
    }

    /**
     * 构建匹配原因
     */
    private String buildMatchReason(TableMatch match, Set<String> keywords) {
        StringBuilder reason = new StringBuilder();
        if (match.getRelevantColumns().size() > 0) {
            reason.append("匹配列: ");
            reason.append(match.getRelevantColumns().stream()
                    .map(c -> c.getColumnName() + "(" + c.getMatchedKeyword() + ")")
                    .collect(Collectors.joining(", ")));
        }
        return reason.toString();
    }

    /**
     * 内部类：表匹配信息
     */
    @lombok.Builder
    @lombok.Data
    private static class TableMatch {
        private String tableName;
        private String remarks;
        private double score;
        private List<RelevantColumn> relevantColumns;
    }
}
