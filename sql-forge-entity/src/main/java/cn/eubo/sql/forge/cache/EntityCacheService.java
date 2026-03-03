package cn.eubo.sql.forge.cache;

import cn.eubo.sql.forge.utils.ReflectionUtils;
import org.springframework.cache.annotation.Cacheable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityCacheService {

    private static final Map<Class<?>, TableStructureInfo> metadataCache = new ConcurrentHashMap<>();

    @Cacheable(value = "tableStructureInfo", key = "#p0")
    public TableStructureInfo getTableInfo(Class<?> entityClass) {
        return metadataCache.computeIfAbsent(entityClass, ReflectionUtils::extractTableInfo);
    }
}
