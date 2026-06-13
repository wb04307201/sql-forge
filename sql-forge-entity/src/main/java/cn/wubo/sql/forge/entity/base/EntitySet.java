package cn.wubo.sql.forge.entity.base;

import cn.wubo.sql.forge.inter.SFunction;

/**
 * 实体字段-值对，用于 INSERT 和 UPDATE 操作中指定字段赋值。
 *
 * @param <T>    实体类型
 * @param column 字段的 Lambda 引用
 * @param value  字段值
 */
public record EntitySet<T>(
        SFunction<T, ?> column,
        Object value
) {
}
