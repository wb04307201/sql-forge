package cn.wubo.sql.forge.mcp.model.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表信息记录类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TableInfo {
    /**
     * 表名
     */
    private String tableName;

    /**
     * 表所属Schema
     */
    private String tableSchema;

    /**
     * 表类型
     */
    private String tableType;

    /**
     * 表备注
     */
    private String remarks;
}
