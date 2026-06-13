package cn.wubo.sql.forge.entity.base;

import cn.wubo.sql.forge.inter.SFunction;
import cn.wubo.sql.forge.enums.ConditionType;

/**
 * 实体条件记录，用于 WHERE 子句中指定列、比较类型和值。
 *
 * @param <T>       实体类型
 * @param column    列的 Lambda 引用
 * @param condition 条件比较类型
 * @param value     条件值
 */
public record EntityCondition<T>(
        SFunction<T, ?> column,
        ConditionType condition,
        Object value
) {
}
