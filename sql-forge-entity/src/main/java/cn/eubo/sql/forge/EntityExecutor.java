package cn.eubo.sql.forge;

import cn.eubo.sql.forge.entity.base.AbstractBase;
import cn.eubo.sql.forge.cache.EntityCacheService;
import cn.wubo.sql.forge.RecordExecutor;

public record EntityExecutor(
        RecordExecutor recordExecutor,
        EntityCacheService entityCacheService
) {

    public <T, R, C extends AbstractBase<T, R, C>> R run(AbstractBase<T, R, C> abstractBase) throws Exception {
        return run("database", abstractBase);
    }

    public <T, R, C extends AbstractBase<T, R, C>> R run(String executorName, AbstractBase<T, R, C> abstractBase) throws Exception {
        return abstractBase.run(executorName, entityCacheService, recordExecutor);
    }

}
