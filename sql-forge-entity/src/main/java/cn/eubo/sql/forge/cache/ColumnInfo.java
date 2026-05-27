package cn.eubo.sql.forge.cache;

import lombok.Data;

import java.lang.reflect.Field;

/**
 * 列信息，描述实体字段与数据库列的映射关系。
 */
@Data
public class ColumnInfo {

    /**
     * 构造方法，初始化空的列信息。
     */
    public ColumnInfo() {
    }

    /** 字段反射对象 */
    private Field field;
    /** Java 字段名 */
    private String fieldName;
    /** 数据库列名 */
    private String columnName;
    /** Java 类型 */
    private Class<?> javaType;
    /** 是否可空 */
    private boolean nullable = true;
    /** 列定义（DDL 片段） */
    private String columnDefinition;
    /** 列长度 */
    private int length;
    /** 精度 */
    private int precision;
    /** 小数位数 */
    private int scale;
    /** 备注 */
    private String comment;
    /** 是否为主键 */
    private boolean isPrimaryKey = false;
    /** 是否为大对象（LOB） */
    private boolean isLob = false;
}
