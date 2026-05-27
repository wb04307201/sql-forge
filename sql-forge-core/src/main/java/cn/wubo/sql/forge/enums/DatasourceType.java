package cn.wubo.sql.forge.enums;

/**
 * 数据源类型枚举，区分普通数据库和 Calcite 联邦查询。
 */
public enum DatasourceType {

    /** 普通数据库 */
    DATABASE("database"),
    /** Apache Calcite 联邦查询 */
    CALCITE("calcite");

    final String value;

    DatasourceType(String value) {
        this.value = value;
    }
}
