package cn.wubo.sql.forge.records;

import java.util.List;

/**
 * 索引信息记录，描述表上的索引定义。
 *
 * @param indexName  索引名称
 * @param nonUnique  是否为非唯一索引
 * @param columnName 索引包含的列名列表
 * @param ascOrDesc  排序方向（"A" 升序 / "D" 降序）
 */
public record IndexInfo(
        String indexName,
        boolean nonUnique,
        List<String> columnName,
        String ascOrDesc
) {
}
