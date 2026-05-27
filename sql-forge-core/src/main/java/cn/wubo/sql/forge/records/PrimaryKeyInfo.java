package cn.wubo.sql.forge.records;

import java.util.List;

/**
 * 主键信息记录，包含主键约束名称和组成列。
 *
 * @param pkName     主键约束名称
 * @param columnName 主键列名列表（支持复合主键）
 */
public record PrimaryKeyInfo(
        String pkName,
        List<String> columnName
) {
}
