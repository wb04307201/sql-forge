package cn.wubo.sql.forge.records;

import java.util.List;

/**
 * 整套表信息记录类
 */
public record EntireTableInfo(
        String tableName,
        String tableSchema,
        String tableType,
        String remarks,
        List<ColumnInfo> columns,
        List<PrimaryKeyInfo> primaryKeys,
        List<ForeignKeyInfo> foreignKeys,
        List<IndexInfo> indexes
) {
}