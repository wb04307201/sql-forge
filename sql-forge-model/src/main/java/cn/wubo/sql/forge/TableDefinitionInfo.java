package cn.wubo.sql.forge;

import java.util.List;

/**
 * 整套表信息记录类
 */
public record TableDefinitionInfo(
        String tableCat,
        String tableSchema,
        String tableName,
        String tableType,
        String remarks,
        List<ColumnInfo> columns,
        List<PrimaryKeyInfo> primaryKeys,
        List<ForeignKeyInfo> foreignKeys,
        List<IndexInfo> indexes
) {
}