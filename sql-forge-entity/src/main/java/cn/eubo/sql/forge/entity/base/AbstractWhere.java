package cn.eubo.sql.forge.entity.base;

import cn.eubo.sql.forge.inter.SFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static cn.wubo.sql.forge.enums.ConditionType.*;

public abstract class AbstractWhere<T, R, C extends AbstractWhere<T, R, C>> extends AbstractBase<T, R, C> {
    protected List<EntityCondition<T>> entityConditions = new ArrayList<>();

    protected AbstractWhere(Class<T> entityClass) {
        super(entityClass);
    }

    public C eq(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, EQ, value));
        return typedThis;
    }

    public C neq(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, NOT_EQ, value));
        return typedThis;
    }

    public C gt(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, GT, value));
        return typedThis;
    }

    public C lt(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, LT, value));
        return typedThis;
    }

    public C gteq(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, GTEQ, value));
        return typedThis;
    }

    public C lteq(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, LTEQ, value));
        return typedThis;
    }

    public C like(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, LIKE, value));
        return typedThis;
    }

    public C notLike(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, NOT_LIKE, value));
        return typedThis;
    }

    public C leftLike(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, LEFT_LIKE, value));
        return typedThis;
    }

    public C rightLike(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, RIGHT_LIKE, value));
        return typedThis;
    }

    public C between(SFunction<T, ?> column, Object value1, Object value2) {
        entityConditions.add(new EntityCondition<>(column, BETWEEN, Arrays.asList(value1, value2)));
        return typedThis;
    }

    public C notBetween(SFunction<T, ?> column, Object value1, Object value2) {
        entityConditions.add(new EntityCondition<>(column, NOT_BETWEEN, Arrays.asList(value1, value2)));
        return typedThis;
    }

    public C in(SFunction<T, ?> column, Object... value) {
        entityConditions.add(new EntityCondition<>(column, IN, Arrays.asList(value)));
        return typedThis;
    }

    public C notIn(SFunction<T, ?> column, Object... value) {
        entityConditions.add(new EntityCondition<>(column, NOT_IN, value));
        return typedThis;
    }

    public C isNull(SFunction<T, ?> column) {
        entityConditions.add(new EntityCondition<>(column, IS_NULL, null));
        return typedThis;
    }

    public C isNotNull(SFunction<T, ?> column) {
        entityConditions.add(new EntityCondition<>(column, IS_NOT_NULL, null));
        return typedThis;
    }
}
