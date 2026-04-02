package cn.wubo.sql.forge.mcp.model.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主键信息记录类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrimaryKeyInfo {
    /**
     * 表名
     */
    private String tableName;

    /**
     * 表所属Schema
     */
    private String tableSchema;

    /**
     * 列名
     */
    private String columnName;

    /**
     * 主键名称
     */
    private String pkName;

    /**
     * 主键序号
     */
    private short keySeq;
}
