package cn.wubo.sql.forge.mcp.model.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据库信息记录类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseInfo {
    /**
     * 数据库名称
     */
    private String databaseName;

    /**
     * 数据库产品名称
     */
    private String productName;

    /**
     * 数据库版本
     */
    private String productVersion;

    /**
     * 驱动名称
     */
    private String driverName;

    /**
     * 驱动版本
     */
    private String driverVersion;
}
