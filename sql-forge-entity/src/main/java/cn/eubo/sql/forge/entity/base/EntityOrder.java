package cn.eubo.sql.forge.entity.base;

import cn.eubo.sql.forge.enums.OrderType;
import cn.eubo.sql.forge.inter.SFunction;

public record EntityOrder<T>(
        SFunction<T, ?> colum,
        OrderType orderType
) {

    public static <T> EntityOrder<T> asc(SFunction<T, ?> colum) {
        return new EntityOrder<>(colum, OrderType.ASC);
    }

    public static <T> EntityOrder<T> desc(SFunction<T, ?> colum) {
        return new EntityOrder<>(colum, OrderType.DESC);
    }
}
