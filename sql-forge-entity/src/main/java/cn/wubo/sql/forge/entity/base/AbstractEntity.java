package cn.wubo.sql.forge.entity.base;

/**
 * 带实体实例引用的抽象构建器，用于按主键操作和 save 场景。
 *
 * @param <T> 实体类型
 * @param <R> 执行结果类型
 * @param <C> 具体子类类型
 */
public abstract class AbstractEntity<T, R, C extends AbstractEntity<T, R, C>> extends AbstractWhere<T, R, C> {

    /** 实体实例 */
    protected T entity;

    /**
     * 构造方法。
     *
     * @param entityClass 实体类对象
     */
    protected AbstractEntity(Class<T> entityClass) {
        super(entityClass);
    }

    /**
     * 设置实体实例。
     *
     * @param entity 实体实例
     * @return 当前构建器
     */
    public C entity(T entity){
        this.entity = entity;
        return typedThis;
    }
}
