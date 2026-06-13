package cn.wubo.sql.forge.inter;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 可序列化的 Lambda 函数接口，用于从方法引用中提取字段名。
 *
 * @param <T> 输入类型（实体类型）
 * @param <R> 返回值类型（字段类型）
 */
@FunctionalInterface
public interface SFunction<T, R> extends Serializable, Function<T, R> {
}
