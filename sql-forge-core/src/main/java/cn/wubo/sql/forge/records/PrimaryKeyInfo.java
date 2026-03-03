package cn.wubo.sql.forge.records;

import java.util.List;

public record PrimaryKeyInfo(
        String pkName,
        List<String> columnName
) {
}
