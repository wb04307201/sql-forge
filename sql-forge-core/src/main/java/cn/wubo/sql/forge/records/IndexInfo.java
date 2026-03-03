package cn.wubo.sql.forge.records;

import java.util.List;

public record IndexInfo(
        String indexName,
        boolean nonUnique,
        List<String> columnName,
        String ascOrDesc
) {
}
