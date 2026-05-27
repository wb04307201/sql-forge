package cn.eubo.sql.forge.entity.base;

import cn.eubo.sql.forge.inter.SFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static cn.wubo.sql.forge.enums.ConditionType.*;

/**
 * 带 WHERE 条件的抽象构建器，提供 eq/neq/gt/lt/like/in 等类型安全的条件方法。
 *
 * @param <T> 实体类型
 * @param <R> 执行结果类型
 * @param <C> 具体子类类型
 */
public abstract class AbstractWhere<T, R, C extends AbstractWhere<T, R, C>> extends AbstractBase<T, R, C> {
    /** WHERE 条件列表 */
    protected List<EntityCondition<T>> entityConditions = new ArrayList<>();

    /**
     * 构造方法。
     *
     * @param entityClass 实体类对象
     */
    protected AbstractWhere(Class<T> entityClass) {
        super(entityClass);
    }

    /**
     * 添加等于条件（=）。
     *
     * @param column 字段引用
     * @param value  比较值
     * @return 当前构建器
     */
    public C eq(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, EQ, value));
        return typedThis;
    }

    /**
     * 添加不等于条件（!=）。
     *
     * @param column 字段引用
     * @param value  比较值
     * @return 当前构建器
     */
    public C neq(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, NOT_EQ, value));
        return typedThis;
    }

    /**
     * 添加大于条件（&gt;）。
     *
     * @param column 字段引用
     * @param value  比较值
     * @return 当前构建器
     */
    public C gt(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, GT, value));
        return typedThis;
    }

    /**
     * 添加小于条件（&lt;）。
     *
     * @param column 字段引用
     * @param value  比较值
     * @return 当前构建器
     */
    public C lt(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, LT, value));
        return typedThis;
    }

    /**
     * 添加大于等于条件（&gt;=）。
     *
     * @param column 字段引用
     * @param value  比较值
     * @return 当前构建器
     */
    public C gteq(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, GTEQ, value));
        return typedThis;
    }

    /**
     * 添加小于等于条件（&lt;=）。
     *
     * @param column 字段引用
     * @param value  比较值
     * @return 当前构建器
     */
    public C lteq(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, LTEQ, value));
        return typedThis;
    }

    /**
     * 添加模糊匹配条件（LIKE）。
     *
     * @param column 字段引用
     * @param value  比较值
     * @return 当前构建器
     */
    public C like(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, LIKE, value));
        return typedThis;
    }

    /**
     * 添加不匹配条件（NOT LIKE）。
     *
     * @param column 字段引用
     * @param value  比较值
     * @return 当前构建器
     */
    public C notLike(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, NOT_LIKE, value));
        return typedThis;
    }

    /**
     * 添加左模糊匹配条件（LIKE %value）。
     *
     * @param column 字段引用
     * @param value  比较值
     * @return 当前构建器
     */
    public C leftLike(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, LEFT_LIKE, value));
        return typedThis;
    }

    /**
     * 添加右模糊匹配条件（LIKE value%）。
     *
     * @param column 字段引用
     * @param value  比较值
     * @return 当前构建器
     */
    public C rightLike(SFunction<T, ?> column, Object value) {
        entityConditions.add(new EntityCondition<>(column, RIGHT_LIKE, value));
        return typedThis;
    }

    /**
     * 添加区间条件（BETWEEN）。
     *
     * @param column 字段引用
     * @param value1 起始值
     * @param value2 结束值
     * @return 当前构建器
     */
    public C between(SFunction<T, ?> column, Object value1, Object value2) {
        entityConditions.add(new EntityCondition<>(column, BETWEEN, Arrays.asList(value1, value2)));
        return typedThis;
    }

    /**
     * 添加不在区间条件（NOT BETWEEN）。
     *
     * @param column 字段引用
     * @param value1 起始值
     * @param value2 结束值
     * @return 当前构建器
     */
    public C notBetween(SFunction<T, ?> column, Object value1, Object value2) {
        entityConditions.add(new EntityCondition<>(column, NOT_BETWEEN, Arrays.asList(value1, value2)));
        return typedThis;
    }

    /**
     * 添加包含条件（IN）。
     *
     * @param column 字段引用
     * @param value  可选值列表
     * @return 当前构建器
     */
    public C in(SFunction<T, ?> column, Object... value) {
        entityConditions.add(new EntityCondition<>(column, IN, Arrays.asList(value)));
        return typedThis;
    }

    /**
     * 添加不包含条件（NOT IN）。
     *
     * @param column 字段引用
     * @param value  可选值列表
     * @return 当前构建器
     */
    public C notIn(SFunction<T, ?> column, Object... value) {
        entityConditions.add(new EntityCondition<>(column, NOT_IN, value));
        return typedThis;
    }

    /**
     * 添加为空条件（IS NULL）。
     *
     * @param column 字段引用
     * @return 当前构建器
     */
    public C isNull(SFunction<T, ?> column) {
        entityConditions.add(new EntityCondition<>(column, IS_NULL, null));
        return typedThis;
    }

    /**
     * 添加不为空条件（IS NOT NULL）。
     *
     * @param column 字段引用
     * @return 当前构建器
     */
    public C isNotNull(SFunction<T, ?> column) {
        entityConditions.add(new EntityCondition<>(column, IS_NOT_NULL, null));
        return typedThis;
    }
}
