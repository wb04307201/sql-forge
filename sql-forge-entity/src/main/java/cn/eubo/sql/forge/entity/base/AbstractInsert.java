package cn.eubo.sql.forge.entity.base;

import cn.eubo.sql.forge.inter.SFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * INSERT 抽象构建器，提供 set 方法指定字段值。
 *
 * @param <T> 实体类型
 * @param <R> 执行结果类型
 * @param <C> 具体子类类型
 */
public abstract class AbstractInsert<T, R, C extends AbstractInsert<T, R, C>> extends AbstractBase<T, R, C> {
    /** 待插入的字段-值列表 */
    protected List<EntitySet<T>> sets = new ArrayList<>();

    /**
     * 构造方法。
     *
     * @param entityClass 实体类对象
     */
    protected AbstractInsert(Class<T> entityClass) {
        super(entityClass);
    }

    /**
     * 设置插入字段的值。
     *
     * @param column 字段的 Lambda 引用
     * @param value  字段值
     * @return 当前构建器
     */
    public C set(SFunction<T, ?> column, Object value) {
        sets.add(new EntitySet<>(column, value));
        return typedThis;
    }
}
