package cn.wubo.sql.forge.mcp.service;

import cn.wubo.sql.forge.mcp.enums.IntentType;
import cn.wubo.sql.forge.mcp.model.QueryIntent;
import cn.wubo.sql.forge.mcp.model.TableSelection;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 意图识别服务
 * 负责分析用户查询意图，提取关键信息
 */
@Service
public class IntentRecognizer {

    // 聚合函数关键词映射
    private static final Map<String, String> AGGREGATE_KEYWORDS = Map.of(
            "count", "COUNT(*)",
            "总数", "COUNT(*)",
            "数量", "COUNT(*)",
            "求和", "SUM",
            "总计", "SUM",
            "平均", "AVG",
            "最大值", "MAX",
            "最大", "MAX",
            "最小值", "MIN",
            "最小", "MIN"
    );

    // 排序关键词
    private static final Map<String, Boolean> ORDER_KEYWORDS = Map.ofEntries(
            Map.entry("从大到小", false),
            Map.entry("降序", false),
            Map.entry("desc", false),
            Map.entry("从小到大", true),
            Map.entry("升序", true),
            Map.entry("asc", true),
            Map.entry("最高", false),
            Map.entry("最低", true),
            Map.entry("最新", false),
            Map.entry("最早", true),
            Map.entry("最近", false),
            Map.entry("最远", true)
    );


    // 逻辑运算符
    private static final Pattern AND_PATTERN = Pattern.compile("(?i)(而且|并且|同时|和|and|且)");
    private static final Pattern OR_PATTERN = Pattern.compile("(?i)(或者|或|or)");
    private static final Pattern NOT_PATTERN = Pattern.compile("(?i)(不是|不含|不包含|没有|not|非)");

    // 比较运算符
    private static final Pattern GT_PATTERN = Pattern.compile("(?i)(大于|超过|多于|高于|>|\\+)\\s*(\\d+)");
    private static final Pattern LT_PATTERN = Pattern.compile("(?i)(小于|少于|低于|不足|<)\\s*(\\d+)");
    private static final Pattern EQ_PATTERN = Pattern.compile("(?i)(等于|为|是|=)\\s*(.+)");
    private static final Pattern BETWEEN_PATTERN = Pattern.compile("(?i)(在.*之间|介于|between)\\s*(\\d+)\\s*(和|至|and|-)\\s*(\\d+)");

    // 分页模式
    private static final Pattern PAGE_PATTERN = Pattern.compile("(?i)(第(\\d+)页|每页(\\d+)条)");
    private static final Pattern LIMIT_PATTERN = Pattern.compile("(?i)(前|前|取|limit)\\s*(\\d+)");

    // 模糊查询模式
    private static final Pattern LIKE_PATTERN = Pattern.compile("(?i)(包含|含有|带|like|包含.*)");

    /**
     * 识别查询意图
     */
    public QueryIntent recognize(String query) {
        if (query == null || query.trim().isEmpty()) {
            return QueryIntent.highConfidence(IntentType.SIMPLE_QUERY, query);
        }

        String trimmedQuery = query.trim();
        double confidence = 0.5;

        // 首先识别主要意图类型
        IntentType primaryIntent = IntentType.fromKeywords(trimmedQuery);
        confidence = calculateConfidence(primaryIntent, trimmedQuery);

        // 提取聚合字段
        List<String> aggregateFields = extractAggregateFields(trimmedQuery);

        // 提取分组字段
        List<String> groupByFields = extractGroupByFields(trimmedQuery, primaryIntent);

        // 提取排序要求
        QueryIntent.SortRequirement sortReq = extractSortRequirement(trimmedQuery);

        // 提取分页要求
        QueryIntent.PaginationRequirement pagination = extractPagination(trimmedQuery);

        // 提取查询条件
        Map<String, Object> conditions = extractConditions(trimmedQuery);

        // 检查是否为模糊意图
        if (isAmbiguousQuery(trimmedQuery, primaryIntent)) {
            return QueryIntent.ambiguous(
                    List.of(primaryIntent),
                    confidence,
                    trimmedQuery
            ).toBuilder()
                    .conditions(conditions)
                    .aggregateFields(aggregateFields)
                    .groupByFields(groupByFields)
                    .sortRequirement(sortReq)
                    .pagination(pagination)
                    .build();
        }

        return QueryIntent.builder()
                .primaryIntent(primaryIntent)
                .confidence(confidence)
                .aggregateFields(aggregateFields)
                .groupByFields(groupByFields)
                .sortRequirement(sortReq)
                .pagination(pagination)
                .conditions(conditions)
                .originalQuery(trimmedQuery)
                .build();
    }

    /**
     * 计算意图置信度
     */
    private double calculateConfidence(IntentType intent, String query) {
        double baseConfidence = 0.5;
        String lowerQuery = query.toLowerCase();

        // 根据关键词数量增加置信度
        int keywordCount = 0;
        for (String keyword : getKeywordsForIntent(intent)) {
            if (lowerQuery.contains(keyword)) {
                keywordCount++;
            }
        }

        // 置信度递增，最高0.95
        baseConfidence = Math.min(0.95, 0.5 + keywordCount * 0.15);

        return baseConfidence;
    }

    /**
     * 获取意图对应的关键词列表
     */
    private List<String> getKeywordsForIntent(IntentType intent) {
        return switch (intent) {
            case COUNT_QUERY -> List.of("有多少", "多少个", "数量", "count", "总数");
            case AGGREGATION_QUERY -> List.of("求和", "平均", "最大", "最小", "sum", "avg", "max", "min");
            case GROUP_BY_QUERY -> List.of("按", "分组", "group", "每个");
            case JOIN_QUERY -> List.of("关联", "连接", "join", "结合");
            case RANKING_QUERY -> List.of("排名", "前", "后", "第几", "top");
            case EXISTS_QUERY -> List.of("是否", "有没有", "exist");
            default -> List.of();
        };
    }

    /**
     * 提取聚合字段
     */
    private List<String> extractAggregateFields(String query) {
        List<String> aggregates = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (Map.Entry<String, String> entry : AGGREGATE_KEYWORDS.entrySet()) {
            if (lowerQuery.contains(entry.getKey())) {
                // 尝试提取字段名
                Pattern fieldPattern = Pattern.compile(entry.getKey() + "\\s*的?\\s*(\\w+)");
                Matcher matcher = fieldPattern.matcher(lowerQuery);
                if (matcher.find()) {
                    aggregates.add(entry.getValue() + "(" + matcher.group(1) + ")");
                } else {
                    aggregates.add(entry.getValue().contains("*") ? "COUNT(*)" : entry.getValue() + "(*)");
                }
            }
        }

        return aggregates;
    }

    /**
     * 提取分组字段
     */
    private List<String> extractGroupByFields(String query, IntentType intent) {
        List<String> groupByFields = new ArrayList<>();

        if (intent == IntentType.GROUP_BY_QUERY || intent == IntentType.HAVING_QUERY) {
            // 提取"按XX分组"中的XX
            Pattern groupPattern = Pattern.compile("(?i)按(\\w+)分组");
            Matcher matcher = groupPattern.matcher(query);
            while (matcher.find()) {
                groupByFields.add(matcher.group(1));
            }

            // 提取"每个XX"
            Pattern eachPattern = Pattern.compile("(?i)每个(\\w+)");
            Matcher eachMatcher = eachPattern.matcher(query);
            while (eachMatcher.find()) {
                if (!groupByFields.contains(eachMatcher.group(1))) {
                    groupByFields.add(eachMatcher.group(1));
                }
            }
        }

        return groupByFields;
    }

    /**
     * 提取排序要求
     */
    private QueryIntent.SortRequirement extractSortRequirement(String query) {
        QueryIntent.SortRequirement.SortRequirementBuilder builder = QueryIntent.SortRequirement.builder();

        for (Map.Entry<String, Boolean> entry : ORDER_KEYWORDS.entrySet()) {
            if (query.contains(entry.getKey())) {
                builder.descending(!entry.getValue()); // false = 降序, true = 升序
                break;
            }
        }

        // 尝试提取排序字段
        Pattern orderFieldPattern = Pattern.compile("(?i)(按|根据)\\s*(\\w+)\\s*排序");
        Matcher matcher = orderFieldPattern.matcher(query);
        if (matcher.find()) {
            builder.orderByFields(List.of(matcher.group(2)));
        }

        return builder.build();
    }

    /**
     * 提取分页要求
     */
    private QueryIntent.PaginationRequirement extractPagination(String query) {
        QueryIntent.PaginationRequirement.PaginationRequirementBuilder builder = QueryIntent.PaginationRequirement.builder();

        // 匹配"第X页"
        Matcher pageMatcher = PAGE_PATTERN.matcher(query);
        if (pageMatcher.find()) {
            String pageStr = pageMatcher.group(2);
            String sizeStr = pageMatcher.group(3);
            int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
            int pageSize = sizeStr != null ? Integer.parseInt(sizeStr) : 10;
            builder.page(page).pageSize(pageSize).offset((page - 1) * pageSize).limit(pageSize);
        }

        // 匹配"前X条"
        Matcher limitMatcher = LIMIT_PATTERN.matcher(query);
        if (limitMatcher.find() && builder.build().getLimit() == 0) {
            int limit = Integer.parseInt(limitMatcher.group(2));
            builder.limit(limit).page(1).pageSize(limit);
        }

        return builder.build();
    }

    /**
     * 提取查询条件
     */
    private Map<String, Object> extractConditions(String query) {
        Map<String, Object> conditions = new HashMap<>();

        // 提取大于条件
        Matcher gtMatcher = GT_PATTERN.matcher(query);
        while (gtMatcher.find()) {
            conditions.put("gt_" + gtMatcher.group(2), gtMatcher.group(2));
        }

        // 提取小于条件
        Matcher ltMatcher = LT_PATTERN.matcher(query);
        while (ltMatcher.find()) {
            conditions.put("lt_" + ltMatcher.group(2), ltMatcher.group(2));
        }

        // 提取等于条件
        Matcher eqMatcher = EQ_PATTERN.matcher(query);
        while (eqMatcher.find()) {
            conditions.put("eq_" + eqMatcher.group(2), eqMatcher.group(2));
        }

        // 提取范围条件
        Matcher betweenMatcher = BETWEEN_PATTERN.matcher(query);
        while (betweenMatcher.find()) {
            Map<String, Object> range = new HashMap<>();
            range.put("min", betweenMatcher.group(2));
            range.put("max", betweenMatcher.group(4));
            conditions.put("range", range);
        }

        // 提取模糊匹配
        Matcher likeMatcher = LIKE_PATTERN.matcher(query);
        if (likeMatcher.find()) {
            conditions.put("like", true);
        }

        return conditions;
    }

    /**
     * 判断是否为模糊意图
     */
    private boolean isAmbiguousQuery(String query, IntentType intent) {
        if (intent == IntentType.SIMPLE_QUERY) {
            // 简单查询但没有明确的表或字段提及
            return query.length() < 10 || !containsAnyFieldMention(query);
        }
        return false;
    }

    /**
     * 判断是否包含字段提及
     */
    private boolean containsAnyFieldMention(String query) {
        // 简单的启发式判断
        return query.matches(".*\\w+.*"); // 至少有一个单词
    }

    /**
     * 生成澄清问题
     */
    public List<String> generateClarificationQuestions(QueryIntent intent) {
        List<String> questions = new ArrayList<>();

        if (intent.getPossibleIntents() != null && !intent.getPossibleIntents().isEmpty()) {
            questions.add("您的需求可能涉及多种查询类型，请确认：");
            for (IntentType possibleIntent : intent.getPossibleIntents()) {
                questions.add("- " + possibleIntent.getDescription());
            }
        }

        if (intent.getPrimaryIntent() == IntentType.JOIN_QUERY &&
                (intent.getCurrentTableSelection() == null ||
                        intent.getCurrentTableSelection().getSelectedTables() == null ||
                        intent.getCurrentTableSelection().getSelectedTables().size() < 2)) {
            questions.add("您的查询涉及多表关联，请确认要关联的表有哪些？");
        }

        if (intent.getPrimaryIntent() == IntentType.AGGREGATION_QUERY &&
                (intent.getAggregateFields() == null || intent.getAggregateFields().isEmpty())) {
            questions.add("您需要进行聚合统计，请问要统计哪些字段？");
        }

        return questions;
    }
}
