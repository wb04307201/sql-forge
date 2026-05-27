package cn.eubo.sql.forge.enums;

import lombok.Getter;

/**
 * 排序方向枚举。
 */
@Getter
public enum OrderType {
    /** 升序 */
    ASC(" asc"),
    /** 降序 */
    DESC(" desc");

    final String value;

    OrderType(String value) {
        this.value = value;
    }
}
