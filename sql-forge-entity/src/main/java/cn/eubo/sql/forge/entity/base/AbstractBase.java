package cn.eubo.sql.forge.entity.base;

import cn.eubo.sql.forge.cache.EntityCacheService;
import cn.wubo.sql.forge.RecordExecutor;

/**
 * 实体操作抽象基类，采用 CRTP 模式支持子类链式调用返回正确类型。
 *
 * @param <T> 实体类型
 * @param <R> 执行结果类型
 * @param <C> 具体子类类型
 */
public abstract class AbstractBase<T, R, C extends AbstractBase<T, R, C>> {

    /** 自引用，用于链式调用返回子类类型 */
    protected final C typedThis = (C) this;

    /** 实体类对象 */
    protected Class<T> entityClass;

    /**
     * 构造方法。
     *
     * @param entityClass 实体类对象
     */
    protected AbstractBase(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * 执行实体操作，由子类实现具体逻辑。
     *
     * @param deleteExecute      执行器名称
     * @param entityCacheService 实体缓存服务
     * @param recordExecutor     Record 层执行器
     * @return 执行结果
     * @throws Exception 执行异常
     */
    public abstract R run(String deleteExecute, EntityCacheService entityCacheService, RecordExecutor recordExecutor) throws Exception;
}
