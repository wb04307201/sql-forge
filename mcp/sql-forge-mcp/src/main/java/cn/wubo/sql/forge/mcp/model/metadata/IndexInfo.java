package cn.wubo.sql.forge.mcp.model.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 索引信息记录类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IndexInfo {
    /**
     * 表名
     */
    private String tableName;

    /**
     * 是否唯一索引
     */
    private boolean nonUnique;

    /**
     * 索引名称
     */
    private String indexName;

    /**
     * 列名
     */
    private String columnName;

    /**
     * 索引类型
     */
    private String indexType;

    /**
     * 排序顺序
     */
    private String ascOrDesc;

    /**
     * 序号
     */
    private int ordinalPosition;
}
