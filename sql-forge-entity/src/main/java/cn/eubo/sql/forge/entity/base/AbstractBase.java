package cn.eubo.sql.forge.entity.base;

import cn.eubo.sql.forge.cache.EntityCacheService;
import cn.wubo.sql.forge.RecordExecutor;

public abstract class AbstractBase<T, R, C extends AbstractBase<T, R, C>> {

    protected final C typedThis = (C) this;

    protected Class<T> entityClass;

    protected AbstractBase(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public abstract R run(String deleteExecute, EntityCacheService entityCacheService, RecordExecutor recordExecutor) throws Exception;
}
