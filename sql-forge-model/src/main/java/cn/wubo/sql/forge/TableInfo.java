package cn.wubo.sql.forge;

/**
 * 表信息记录类
 */
public record TableInfo(
        String tableCat,
        String tableSchema,
        String tableName,
        String tableType,
        String remarks
) {
}