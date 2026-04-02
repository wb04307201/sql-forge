package cn.wubo.sql.forge.mcp.service;

import cn.wubo.sql.forge.mcp.enums.IntentType;
import cn.wubo.sql.forge.mcp.model.QueryIntent;
import cn.wubo.sql.forge.mcp.model.TableSelection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 模糊意图解析器
 * 处理用户查询中的歧义和模糊表达，提供多种可能的SQL解释
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmbiguityResolver {

    private final IntentRecognizer intentRecognizer;
    private final TableSelector tableSelector;

    // 模糊表达模式
    private static final List<AmbiguityPattern> AMBIGUITY_PATTERNS = Arrays.asList(
            // 数量模糊
            new AmbiguityPattern(
                    Pattern.compile("(?i)(所有|全部|全部的|所有的)"),
                    "返回表中所有符合条件的数据"
            ),
            // 时间模糊
            new AmbiguityPattern(
                    Pattern.compile("(?i)(最近|近期|最近一段时间|最近几天)"),
                    "需要确定具体的时间范围"
            ),
            // 排名模糊
            new AmbiguityPattern(
                    Pattern.compile("(?i)(前.*名|前.*个|排.*|名次)"),
                    "需要确定具体的排名数量"
            ),
            // 排序模糊
            new AmbiguityPattern(
                    Pattern.compile("(?i)(最高的|最低的|最好的|最差的)"),
                    "需要确定排序字段"
            ),
            // 范围模糊
            new AmbiguityPattern(
                    Pattern.compile("(?i)(大.*|小.*|多.*|少.*|高.*|低.*)"),
                    "需要确定具体的数值范围"
            )
    );

    /**
     * 解析模糊意图，返回多个可能的解释
     */
    public List<AmbiguityInterpretation> resolveAmbiguity(String query, String schemaJson, QueryIntent intent) {
        List<AmbiguityInterpretation> interpretations = new ArrayList<>();

        // 1. 分析模糊表达
        List<String> vagueExpressions = findVagueExpressions(query);
        List<String> clarificationQuestions = generateClarificationQuestions(query, vagueExpressions);

        // 2. 生成多种可能的解释
        interpretations.add(generateMostLikelyInterpretation(query, intent));
        interpretations.addAll(generateAlternativeInterpretations(query, intent, schemaJson));

        // 3. 对每种解释生成可能的SQL
        for (AmbiguityInterpretation interpretation : interpretations) {
            interpretation.setClarificationQuestions(clarificationQuestions);
        }

        return interpretations;
    }

    /**
     * 查找模糊表达
     */
    private List<String> findVagueExpressions(String query) {
        List<String> vagueExpressions = new ArrayList<>();
        for (AmbiguityPattern pattern : AMBIGUITY_PATTERNS) {
            Matcher matcher = pattern.pattern.matcher(query);
            while (matcher.find()) {
                vagueExpressions.add(matcher.group());
            }
        }
        return vagueExpressions;
    }

    /**
     * 生成澄清问题
     */
    private List<String> generateClarificationQuestions(String query, List<String> vagueExpressions) {
        List<String> questions = new ArrayList<>();

        if (vagueExpressions.isEmpty()) {
            return questions;
        }

        for (String expr : vagueExpressions) {
            if (query.contains("所有") || query.contains("全部")) {
                questions.add("您想要查询所有记录吗？还是有其他筛选条件？");
            }
            if (query.contains("最近")) {
                questions.add("请问您想查询多长时间范围内的数据？（如：最近一周、最近一个月等）");
            }
            if (query.contains("前") && (query.contains("名") || query.contains("个"))) {
                questions.add("请问您想查询前多少名/多少条记录？（如：前10名）");
            }
            if (query.contains("最高") || query.contains("最低") || query.contains("最好") || query.contains("最差")) {
                questions.add("请问您想按哪个字段进行排序？");
            }
        }

        return questions.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 生成最可能的解释
     */
    private AmbiguityInterpretation generateMostLikelyInterpretation(String query, QueryIntent intent) {
        return AmbiguityInterpretation.builder()
                .interpretationId(UUID.randomUUID().toString())
                .probability(0.6)
                .reason("基于关键词识别的最可能解释")
                .assumptions(List.of(
                        "假设用户想要返回合理数量的结果",
                        "假设时间范围为最近30天",
                        "假设排名前10"
                ))
                .suggestedSqlPattern(buildSuggestedPattern(query, intent, true))
                .build();
    }

    /**
     * 生成替代解释
     */
    private List<AmbiguityInterpretation> generateAlternativeInterpretations(String query,
                                                                               QueryIntent intent,
                                                                               String schemaJson) {
        List<AmbiguityInterpretation> alternatives = new ArrayList<>();

        // 基于上下文生成替代解释
        if (query.contains("前")) {
            alternatives.add(AmbiguityInterpretation.builder()
                    .interpretationId(UUID.randomUUID().toString())
                    .probability(0.25)
                    .reason("另一种可能的解释")
                    .assumptions(List.of("假设用户想要前5条记录"))
                    .suggestedSqlPattern(buildSuggestedPattern(query, intent, false))
                    .build());
        }

        if (query.contains("所有") || query.contains("全部")) {
            alternatives.add(AmbiguityInterpretation.builder()
                    .interpretationId(UUID.randomUUID().toString())
                    .probability(0.15)
                    .reason("全部查询的解释")
                    .assumptions(List.of("假设用户想要所有符合条件的记录"))
                    .suggestedSqlPattern(buildSuggestedPattern(query, intent, false))
                    .build());
        }

        return alternatives;
    }

    /**
     * 构建建议的SQL模式
     */
    private String buildSuggestedPattern(String query, QueryIntent intent, boolean withLimits) {
        StringBuilder pattern = new StringBuilder("SELECT ");

        if (intent != null && intent.getAggregateFields() != null && !intent.getAggregateFields().isEmpty()) {
            pattern.append(String.join(", ", intent.getAggregateFields()));
        } else {
            pattern.append("*");
        }

        pattern.append(" FROM <table>");

        if (query.contains("前") || withLimits) {
            pattern.append(" ORDER BY <sort_field> DESC");
            if (withLimits) {
                pattern.append(" LIMIT ").append(extractLimitIfPresent(query)).append(" OR OFFSET 0");
            }
        }

        return pattern.toString();
    }

    /**
     * 提取限制数量
     */
    private int extractLimitIfPresent(String query) {
        Pattern limitPattern = Pattern.compile("前(\\d+)");
        Matcher matcher = limitPattern.matcher(query);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 10; // 默认10条
    }

    /**
     * 基于用户反馈消除歧义
     */
    public QueryIntent resolveFromFeedback(String originalQuery, String feedback,
                                            QueryIntent originalIntent, String schemaJson) {
        String lowerFeedback = feedback.toLowerCase();

        // 分析用户反馈
        if (feedback.matches("\\d+") || feedback.matches("前?\\d+条?")) {
            // 用户提供了具体数量
            int limit = extractLimit(feedback);
            if (originalIntent.getPagination() != null) {
                originalIntent.getPagination().setLimit(limit);
            } else {
                originalIntent.setPagination(QueryIntent.PaginationRequirement.builder()
                        .limit(limit)
                        .page(1)
                        .pageSize(limit)
                        .build());
            }
        }

        if (feedback.contains("天") || feedback.contains("月") || feedback.contains("周") || feedback.contains("年")) {
            // 用户提供了时间范围
            originalIntent.getConditions().put("timeRange", extractTimeRange(feedback));
        }

        if (feedback.contains("按") || feedback.contains("排序")) {
            // 用户指定了排序字段
            String sortField = extractSortField(feedback);
            if (originalIntent.getSortRequirement() != null) {
                originalIntent.getSortRequirement().setOrderByFields(List.of(sortField));
            } else {
                originalIntent.setSortRequirement(QueryIntent.SortRequirement.builder()
                        .orderByFields(List.of(sortField))
                        .descending(true)
                        .build());
            }
        }

        // 重新识别意图
        return intentRecognizer.recognize(originalQuery + " " + feedback);
    }

    private int extractLimit(String feedback) {
        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(feedback);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 10;
    }

    private String extractTimeRange(String feedback) {
        Pattern pattern = Pattern.compile("(\\d+)\\s*(天|月|周|年)");
        Matcher matcher = pattern.matcher(feedback);
        if (matcher.find()) {
            return matcher.group(1) + matcher.group(2);
        }
        return "30天";
    }

    private String extractSortField(String feedback) {
        // 简单实现，实际需要更复杂的处理
        if (feedback.contains("按")) {
            int start = feedback.indexOf("按") + 1;
            int end = feedback.indexOf("排序");
            if (end > start) {
                return feedback.substring(start, end).trim();
            }
        }
        return "id";
    }

    /**
     * 模糊表达模式
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class AmbiguityPattern {
        private Pattern pattern;
        private String description;
    }

    /**
     * 歧义解释
     */
    @lombok.Data
    @lombok.Builder
    public static class AmbiguityInterpretation {
        private String interpretationId;
        private double probability;
        private String reason;
        private List<String> assumptions;
        private String suggestedSqlPattern;
        private List<String> clarificationQuestions;
    }
}
