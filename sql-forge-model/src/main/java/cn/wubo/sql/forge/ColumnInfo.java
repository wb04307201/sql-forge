
package cn.wubo.sql.forge;

/**
 * 列信息记录类
 */
public record ColumnInfo(
        String columnName,
        String typeName,
        int columnSize,
        int decimalDigits,
        String remarks,
        String columnDef,
        int ordinalPosition,
        String isNullable,
        String isAutoincrement,
        String isGeneratedcolumn
) {}
