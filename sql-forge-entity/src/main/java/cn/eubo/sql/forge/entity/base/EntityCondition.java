package cn.eubo.sql.forge.entity.base;

import cn.eubo.sql.forge.inter.SFunction;
import cn.wubo.sql.forge.enums.ConditionType;

public record EntityCondition<T>(
        SFunction<T, ?> column,
        ConditionType condition,
        Object value
) {
}
