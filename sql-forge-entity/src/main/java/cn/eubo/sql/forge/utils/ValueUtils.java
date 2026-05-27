package cn.eubo.sql.forge.utils;

import lombok.experimental.UtilityClass;

/**
 * 值类型转换工具类，将通用对象值转换为目标 Java 类型。
 */
@UtilityClass
public class ValueUtils {

    /**
     * 将值转换为指定的目标类型（支持 Integer、Long、String、Boolean）。
     *
     * @param value      原始值
     * @param targetType 目标 Java 类型
     * @return 转换后的值
     */
    public Object convertValueToFieldType(Object value, Class<?> targetType) {
        if (value == null) return null;

        if (targetType == Integer.class || targetType == int.class) {
            return ((Number) value).intValue();
        } else if (targetType == Long.class || targetType == long.class) {
            return ((Number) value).longValue();
        } else if (targetType == String.class) {
            return value.toString();
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.valueOf(value.toString());
        }
        return value;
    }
}
