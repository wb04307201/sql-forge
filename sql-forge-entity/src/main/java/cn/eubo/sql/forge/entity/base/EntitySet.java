package cn.eubo.sql.forge.entity.base;

import cn.eubo.sql.forge.inter.SFunction;

public record EntitySet<T>(
        SFunction<T, ?> column,
        Object value
) {
}
