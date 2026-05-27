package cn.eubo.sql.forge.entity.base;

import cn.eubo.sql.forge.inter.SFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * UPDATE 抽象构建器，提供 set 方法和 WHERE 条件。
 *
 * @param <T> 实体类型
 * @param <R> 执行结果类型
 * @param <C> 具体子类类型
 */
public abstract class AbstractUpdate<T, R, C extends AbstractUpdate<T, R, C>> extends AbstractWhere<T, R, C> {
    /** 待更新的字段-值列表 */
    protected List<EntitySet<T>> sets = new ArrayList<>();

    /**
     * 构造方法。
     *
     * @param entityClass 实体类对象
     */
    protected AbstractUpdate(Class<T> entityClass) {
        super(entityClass);
    }

    /**
     * 设置更新字段的值。
     *
     * @param column 字段的 Lambda 引用
     * @param value  新值
     * @return 当前构建器
     */
    public C set(SFunction<T, ?> column, Object value) {
        sets.add(new EntitySet<>(column, value));
        return typedThis;
    }
}
