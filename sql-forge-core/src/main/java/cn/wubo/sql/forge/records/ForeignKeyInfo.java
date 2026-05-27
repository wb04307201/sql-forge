package cn.wubo.sql.forge.records;

/**
 * 外键信息记录，描述表间外键引用关系。
 *
 * @param fkName      外键约束名称
 * @param pkName      被引用的主键约束名称
 * @param pkTableName 被引用的主表名称
 * @param pkColumnName 被引用的主键列名
 * @param fkTableName 外键所在的从表名称
 * @param fkColumnName 外键列名
 */
public record ForeignKeyInfo(
        String fkName,
        String pkName,
        String pkTableName,
        String pkColumnName,
        String fkTableName,
        String fkColumnName
) {
}
