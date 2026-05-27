package cn.eubo.sql.forge;

import cn.eubo.sql.forge.entity.base.AbstractBase;
import cn.eubo.sql.forge.cache.EntityCacheService;
import cn.wubo.sql.forge.RecordExecutor;

/**
 * 实体操作执行器，桥接 Entity 层与 Record 层，将类型安全的实体操作委托给 RecordExecutor 执行。
 */
public record EntityExecutor(
        RecordExecutor recordExecutor,
        EntityCacheService entityCacheService
) {

    /**
     * 使用默认执行器（"database"）执行实体操作。
     *
     * @param abstractBase 实体操作构建器
     * @param <T>          实体类型
     * @param <R>          结果类型
     * @param <C>          构建器类型
     * @return 执行结果
     * @throws Exception 执行异常
     */
    public <T, R, C extends AbstractBase<T, R, C>> R run(AbstractBase<T, R, C> abstractBase) throws Exception {
        return run("database", abstractBase);
    }

    /**
     * 使用指定执行器执行实体操作。
     *
     * @param executorName 执行器名称
     * @param abstractBase 实体操作构建器
     * @param <T>          实体类型
     * @param <R>          结果类型
     * @param <C>          构建器类型
     * @return 执行结果
     * @throws Exception 执行异常
     */
    public <T, R, C extends AbstractBase<T, R, C>> R run(String executorName, AbstractBase<T, R, C> abstractBase) throws Exception {
        return abstractBase.run(executorName, entityCacheService, recordExecutor);
    }

}
