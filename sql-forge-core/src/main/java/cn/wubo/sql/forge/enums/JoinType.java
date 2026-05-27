package cn.wubo.sql.forge.enums;

/**
 * SQL JOIN 类型枚举，支持 INNER、LEFT、RIGHT、OUTER 等连接方式。
 */
public enum JoinType {
    JOIN,
    INNER_JOIN,
    LEFT_OUTER_JOIN,
    RIGHT_OUTER_JOIN,
    OUTER_JOIN
}