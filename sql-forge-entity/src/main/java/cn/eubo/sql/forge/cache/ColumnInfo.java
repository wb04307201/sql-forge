package cn.eubo.sql.forge.cache;

import lombok.Data;

import java.lang.reflect.Field;

@Data
public class ColumnInfo {
    private Field field;
    private String fieldName;
    private String columnName;
    private Class<?> javaType;
    private boolean nullable = true;
    private String columnDefinition;
    private int length;
    private int precision;
    private int scale;
    private String comment;
    private boolean isPrimaryKey = false;
    private boolean isLob = false;
}
