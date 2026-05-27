package cn.eubo.sql.forge.entity.base;

import cn.eubo.sql.forge.enums.OrderType;
import cn.eubo.sql.forge.inter.SFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * SELECT 抽象构建器，提供列选择、排序、去重等方法。
 *
 * @param <T> 实体类型
 * @param <R> 执行结果类型
 * @param <C> 具体子类类型
 */
public abstract class AbstractSelect<T, R, C extends AbstractSelect<T, R, C>> extends AbstractWhere<T, R, C> {
    /** 查询列列表 */
    protected List<SFunction<T, ?>> columns = new ArrayList<>();
    /** 排序列列表 */
    protected List<EntityOrder<T>> orders = new ArrayList<>();
    /** 是否去重 */
    protected boolean distinct;

    /**
     * 构造方法。
     *
     * @param entityClass 实体类对象
     */
    protected AbstractSelect(Class<T> entityClass) {
        super(entityClass);
    }

    /**
     * 添加单个查询列。
     *
     * @param column 列的 Lambda 引用
     * @return 当前构建器
     */
    public C column(SFunction<T, ?> column) {
        this.columns.add(column);
        return typedThis;
    }

    /**
     * 添加多个查询列。
     *
     * @param columns 列的 Lambda 引用数组
     * @return 当前构建器
     */
    public C columns(SFunction<T, ?>... columns) {
        this.columns.addAll(List.of(columns));
        return typedThis;
    }

    /**
     * 添加升序排序列。
     *
     * @param column 排序列的 Lambda 引用
     * @return 当前构建器
     */
    public C orderAsc(SFunction<T, ?> column) {
        this.orders.add(new EntityOrder<>(column, OrderType.ASC));
        return typedThis;
    }

    /**
     * 添加降序排序列。
     *
     * @param column 排序列的 Lambda 引用
     * @return 当前构建器
     */
    public C orderDesc(SFunction<T, ?> column) {
        this.orders.add(new EntityOrder<>(column, OrderType.DESC));
        return typedThis;
    }

    /**
     * 添加多个升序排序列。
     *
     * @param columns 排序列的 Lambda 引用数组
     * @return 当前构建器
     */
    public C orders(SFunction<T, ?>... columns) {
        for (SFunction<T, ?> column : columns) {
            this.orders.add(new EntityOrder<>(column, OrderType.ASC));
        }
        return typedThis;
    }

    /**
     * 设置是否去重。
     *
     * @param distinct 是否去重
     * @return 当前构建器
     */
    public C distinct(Boolean distinct) {
        this.distinct = distinct;
        return typedThis;
    }
}
