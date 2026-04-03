package cn.wubo.sql.forge.mcp.model;

import cn.wubo.sql.forge.mcp.enums.IntentType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 查询意图识别结果
 */
@Data
@Builder(toBuilder = true)
public class QueryIntent {
    /**
     * 主要意图类型
     */
    private IntentType primaryIntent;

    /**
     * 可能的意图列表（模糊意图时可能有多个）
     */
    private List<IntentType> possibleIntents;

    /**
     * 意图置信度
     */
    private double confidence;

    /**
     * 提取的查询条件
     */
    private Map<String, Object> conditions;

    /**
     * 聚合字段
     */
    private List<String> aggregateFields;

    /**
     * 分组字段
     */
    private List<String> groupByFields;

    /**
     * 排序要求
     */
    private SortRequirement sortRequirement;

    /**
     * 分页要求
     */
    private PaginationRequirement pagination;

    /**
     * 模糊意图的澄清问题
     */
    private List<String> clarificationQuestions;

    /**
     * 原始查询文本
     */
    private String originalQuery;

    /**
     * 当前选择的表（用于 JOIN 查询验证）
     */
    private TableSelection currentTableSelection;

    /**
     * 创建高置信度意图
     */
    public static QueryIntent highConfidence(IntentType intent, String query) {
        return QueryIntent.builder()
                .primaryIntent(intent)
                .confidence(1.0)
                .originalQuery(query)
                .build();
    }

    /**
     * 创建模糊意图（需要用户澄清）
     */
    public static QueryIntent ambiguous(List<IntentType> intents, double confidence, String query) {
        return QueryIntent.builder()
                .primaryIntent(IntentType.UNCLEAR_INTENT)
                .possibleIntents(intents)
                .confidence(confidence)
                .originalQuery(query)
                .build();
    }

    /**
     * 是否为模糊意图（使用默认阈值0.7）
     * 实际阈值在NL2SQLService中配置
     */
    public boolean isAmbiguous() {
        return primaryIntent == IntentType.UNCLEAR_INTENT || confidence < 0.7;
    }

    /**
     * 获取意图描述
     */
    public String getIntentDescription() {
        if (primaryIntent != null) {
            return primaryIntent.getName() + " - " + primaryIntent.getDescription();
        }
        return "未识别意图";
    }

    /**
     * 排序要求
     */
    @Data
    @Builder
    public static class SortRequirement {
        private List<String> orderByFields;
        private boolean descending;
        private String nullsOrder; // FIRST 或 LAST
    }

    /**
     * 分页要求
     */
    @Data
    @Builder
    public static class PaginationRequirement {
        private int page;
        private int pageSize;
        private int offset;
        private int limit;
    }
}
