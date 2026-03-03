
package cn.wubo.sql.forge.records;

/**
 * 列信息记录类
 */
public record ColumnInfo(
        String columnName,
        String typeName,
        int columnSize,
        int decimalDigits,
        int nullable,
        String remarks,
        String columnDef,
        int ordinalPosition,
        String isNullable,
        String isAutoincrement,
        String isGeneratedcolumn
) {}
