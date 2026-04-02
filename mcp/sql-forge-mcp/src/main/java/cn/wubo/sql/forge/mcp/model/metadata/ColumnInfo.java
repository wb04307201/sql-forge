package cn.wubo.sql.forge.mcp.model.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 列信息记录类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ColumnInfo {
    /**
     * 列名
     */
    private String columnName;

    /**
     * 数据类型
     */
    private String typeName;

    /**
     * 列大小
     */
    private int columnSize;

    /**
     * 小数位数
     */
    private int decimalDigits;

    /**
     * 是否可为空
     */
    private int nullable;

    /**
     * 列备注
     */
    private String remarks;

    /**
     * 默认值
     */
    private String columnDef;

    /**
     * 序号位置
     */
    private int ordinalPosition;

    /**
     * 是否可为空字符串
     */
    private String isNullable;

    /**
     * 是否自增
     */
    private String isAutoincrement;

    /**
     * 是否为计算列
     */
    private String isGeneratedcolumn;
}
