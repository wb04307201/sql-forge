package cn.wubo.sql.forge.mcp.model.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 外键信息记录类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForeignKeyInfo {
    /**
     * 外键名称
     */
    private String fkName;

    /**
     * 主键表名
     */
    private String pktableName;

    /**
     * 主键列名
     */
    private String pkcolumnName;

    /**
     * 外键表名
     */
    private String fktableName;

    /**
     * 外键列名
     */
    private String fkcolumnName;

    /**
     * 主键序号
     */
    private short keySeq;

    /**
     * 更新规则
     */
    private short updateRule;

    /**
     * 删除规则
     */
    private short deleteRule;
}
