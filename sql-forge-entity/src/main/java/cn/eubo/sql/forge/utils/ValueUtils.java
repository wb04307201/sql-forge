package cn.eubo.sql.forge.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ValueUtils {

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
