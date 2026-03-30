package cn.wubo.sql.forge.records;

/**
 * 表信息记录类
 */
public record TableInfo(
        String tableName,
        String tableSchema,
        String tableType,
        String remarks
) {
}