package cn.eubo.sql.forge.entity.base;

/**
 * DELETE 抽象构建器，继承 WHERE 条件能力。
 *
 * @param <T> 实体类型
 * @param <R> 执行结果类型
 * @param <C> 具体子类类型
 */
public abstract class AbstractDelete<T, R, C extends AbstractDelete<T, R, C>> extends AbstractWhere<T, R, C> {

    /**
     * 构造方法。
     *
     * @param entityClass 实体类对象
     */
    protected AbstractDelete(Class<T> entityClass) {
        super(entityClass);
    }
}
