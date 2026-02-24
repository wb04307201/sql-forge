package cn.eubo.sql.forge.entity.base;

import cn.eubo.sql.forge.enums.OrderType;
import cn.eubo.sql.forge.inter.SFunction;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSelect<T, R, C extends AbstractSelect<T, R, C>> extends AbstractWhere<T, R, C> {
    protected List<SFunction<T, ?>> columns = new ArrayList<>();
    protected List<EntityOrder<T>> orders = new ArrayList<>();
    protected boolean distinct;

    protected AbstractSelect(Class<T> entityClass) {
        super(entityClass);
    }

    public C column(SFunction<T, ?> column) {
        this.columns.add(column);
        return typedThis;
    }

    public C columns(SFunction<T, ?>... columns) {
        this.columns.addAll(List.of(columns));
        return typedThis;
    }

    public C orderAsc(SFunction<T, ?> column) {
        this.orders.add(new EntityOrder<>(column, OrderType.ASC));
        return typedThis;
    }

    public C orderDesc(SFunction<T, ?> column) {
        this.orders.add(new EntityOrder<>(column, OrderType.DESC));
        return typedThis;
    }

    public C orders(SFunction<T, ?>... columns) {
        for (SFunction<T, ?> column : columns) {
            this.orders.add(new EntityOrder<>(column, OrderType.ASC));
        }
        return typedThis;
    }

    public C distinct(Boolean distinct) {
        this.distinct = distinct;
        return typedThis;
    }
}
