package cn.wubo.sql.forge.records;

public record ForeignKeyInfo(
        String fkName,
        String pkName,
        String pkTableName,
        String pkColumnName,
        String fkTableName,
        String fkColumnName
) {
}
