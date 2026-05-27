package cn.wubo.sql.forge.enums;

import lombok.Getter;

/**
 * WHERE 条件比较类型枚举，定义 SQL 中支持的所有条件运算符。
 */
@Getter
public enum ConditionType {

    EQ(" = "),                   // 等于
    NOT_EQ(" <> "),              // 不等于
    LIKE(" LIKE "),              // 模糊匹配
    NOT_LIKE(" NOT LIKE "),      // 不匹配（NOT LIKE）
    LEFT_LIKE(" LIKE "),         // 左模糊匹配
    RIGHT_LIKE(" LIKE "),        // 右模糊匹配
    GT(" > "),                   // 大于
    LT(" < "),                   // 小于
    GTEQ(" >= "),                // 大于等于
    LTEQ(" <= "),                // 小于等于
    BETWEEN(" BETWEEN "),        // 区间匹配
    NOT_BETWEEN(" NOT BETWEEN "),// 不在区间内
    IN(" IN "),                  // 包含于集合
    NOT_IN(" NOT IN "),          // 不包含于集合
    IS_NULL(" IS NULL "),        // 为空
    IS_NOT_NULL(" IS NOT NULL ");// 不为空

    final String value;

    ConditionType(String value) {
        this.value = value;
    }
}

