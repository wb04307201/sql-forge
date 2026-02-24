package cn.eubo.sql.forge.entity.base;

import cn.eubo.sql.forge.inter.SFunction;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractUpdate<T, R, C extends AbstractUpdate<T, R, C>> extends AbstractWhere<T, R, C> {
    protected List<EntitySet<T>> sets = new ArrayList<>();

    protected AbstractUpdate(Class<T> entityClass) {
        super(entityClass);
    }

    public C set(SFunction<T, ?> column, Object value) {
        sets.add(new EntitySet<>(column, value));
        return typedThis;
    }
}
