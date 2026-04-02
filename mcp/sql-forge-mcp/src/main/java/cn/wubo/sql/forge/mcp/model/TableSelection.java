package cn.wubo.sql.forge.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 表选择结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableSelection {
    /**
     * 选择的表信息
     */
    private List<SelectedTable> selectedTables;

    /**
     * 表之间的关联关系
     */
    private List<TableRelation> relations;

    /**
     * 整体相关性评分
     */
    private double totalScore;

    /**
     * 选择的表信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectedTable {
        /**
         * 表名
         */
        private String tableName;

        /**
         * 表描述/备注
         */
        private String remarks;

        /**
         * 相关性评分 (0-1)
         */
        private double relevanceScore;

        /**
         * 表中相关列
         */
        private List<RelevantColumn> relevantColumns;

        /**
         * 是否为模糊匹配
         */
        private boolean fuzzyMatched;

        /**
         * 匹配原因
         */
        private String matchReason;
    }

    /**
     * 相关列信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelevantColumn {
        /**
         * 列名
         */
        private String columnName;

        /**
         * 列类型
         */
        private String columnType;

        /**
         * 相关性评分 (0-1)
         */
        private double relevanceScore;

        /**
         * 列用途（SELECT/JOIN/WHERE）
         */
        private ColumnUsage usage;

        /**
         * 匹配关键词
         */
        private String matchedKeyword;
    }

    /**
     * 列用途枚举
     */
    public enum ColumnUsage {
        SELECT,      // 查询字段
        JOIN,        // 关联字段
        WHERE,       // 条件字段
        ORDER_BY,    // 排序字段
        GROUP_BY     // 分组字段
    }

    /**
     * 表关联关系
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableRelation {
        /**
         * 关联类型
         */
        private String joinType;

        /**
         * 左表
         */
        private String leftTable;

        /**
         * 左表列
         */
        private String leftColumn;

        /**
         * 右表
         */
        private String rightTable;

        /**
         * 右表列
         */
        private String rightColumn;

        /**
         * 关联来源（外键/语义相似度）
         */
        private String source;
    }
}
