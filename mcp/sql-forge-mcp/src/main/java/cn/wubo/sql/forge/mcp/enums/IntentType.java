package cn.wubo.sql.forge.mcp.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 查询意图类型枚举
 */
@Getter
@RequiredArgsConstructor
public enum IntentType {
    // 基础查询
    SIMPLE_QUERY("简单查询", "直接查询表数据"),
    COUNT_QUERY("计数查询", "统计记录数量"),
    EXISTS_QUERY("存在性查询", "检查是否存在满足条件的记录"),

    // 聚合分析
    AGGREGATION_QUERY("聚合查询", "使用COUNT/SUM/AVG/MIN/MAX等聚合函数"),
    GROUP_BY_QUERY("分组查询", "按字段分组统计"),
    HAVING_QUERY("HAVING查询", "分组后条件过滤"),
    RANKING_QUERY("排名查询", "查询排名靠前/后的记录"),

    // 多表关联
    JOIN_QUERY("关联查询", "多表JOIN操作"),
    SUBQUERY_QUERY("子查询", "嵌套子查询"),
    UNION_QUERY("UNION查询", "合并多个结果集"),

    // 复杂分析
    PIVOT_QUERY("透视查询", "行列转换"),
    RECURSIVE_QUERY("递归查询", "树形结构查询"),
    WINDOW_FUNCTION_QUERY("窗口函数查询", "使用ROW_NUMBER/RANK等窗口函数"),

    // 数据操作
    INSERT_QUERY("插入数据", "INSERT语句"),
    UPDATE_QUERY("更新数据", "UPDATE语句"),
    DELETE_QUERY("删除数据", "DELETE语句"),

    // 模糊意图
    UNCLEAR_INTENT("模糊意图", "无法明确识别的查询");

    private final String name;
    private final String description;

    /**
     * 从关键词推断意图类型
     */
    public static IntentType fromKeywords(String query) {
        String lowerQuery = query.toLowerCase();

        // 计数查询
        if (containsAny(lowerQuery, "有多少", "多少个", "数量", "count", "总数", "总计")) {
            return COUNT_QUERY;
        }

        // 存在性查询
        if (containsAny(lowerQuery, "是否存在", "有没有", "是否存在", "exist", "exists")) {
            return EXISTS_QUERY;
        }

        // 聚合函数
        if (containsAny(lowerQuery, "求和", "平均", "最大", "最小", "sum", "avg", "max", "min", "总计", "平均")) {
            return AGGREGATION_QUERY;
        }

        // 分组查询
        if (containsAny(lowerQuery, "按", "分组", "group", "每个", "各个")) {
            return GROUP_BY_QUERY;
        }

        // HAVING查询
        if (containsAny(lowerQuery, "大于", "小于", "超过", "少于", "having")) {
            return HAVING_QUERY;
        }

        // 排名查询
        if (containsAny(lowerQuery, "排名", "前", "后", "第几", "rank", "top", "limit")) {
            return RANKING_QUERY;
        }

        // JOIN查询
        if (containsAny(lowerQuery, "关联", "连接", "join", "结合")) {
            return JOIN_QUERY;
        }

        // 子查询
        if (containsAny(lowerQuery, "子查询", "嵌套", "select.*select", "subquery")) {
            return SUBQUERY_QUERY;
        }

        // UNION查询
        if (containsAny(lowerQuery, "合并", "union", "同时查询")) {
            return UNION_QUERY;
        }

        // 递归查询
        if (containsAny(lowerQuery, "递归", "树形", "parent", "children", "层级")) {
            return RECURSIVE_QUERY;
        }

        // 窗口函数
        if (containsAny(lowerQuery, "窗口", "累计", "running", "row_number", "rank", "dense_rank")) {
            return WINDOW_FUNCTION_QUERY;
        }

        // 数据操作
        if (containsAny(lowerQuery, "插入", "新增", "insert", "add")) {
            return INSERT_QUERY;
        }
        if (containsAny(lowerQuery, "更新", "修改", "修改", "update", "set")) {
            return UPDATE_QUERY;
        }
        if (containsAny(lowerQuery, "删除", "remove", "delete")) {
            return DELETE_QUERY;
        }

        return SIMPLE_QUERY;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
