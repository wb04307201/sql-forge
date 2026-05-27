package cn.wubo.sql.forge.enums;

/**
 * SQL 语句类型枚举，用于 {@link cn.wubo.sql.forge.jdbc.AbstractSQL} 内部标识当前构建的语句类型。
 */
public enum StatementType {
    DELETE,
    INSERT,
    SELECT,
    UPDATE,
    MERGE
}
