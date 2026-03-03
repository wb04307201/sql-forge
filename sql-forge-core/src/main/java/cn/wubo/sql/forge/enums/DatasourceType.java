package cn.wubo.sql.forge.enums;

public enum DatasourceType {

    DATABASE("database"),
    CALCITE("calcite");

    final String value;

    DatasourceType(String value) {
        this.value = value;
    }
}
