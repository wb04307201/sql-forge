package cn.wubo.sql.forge.records;

import java.util.List;

/**
 * 整套表信息记录类
 */
public record EntireTable(
        String tableName,
        String tableSchema,
        String tableType,
        String remarks
) {
}