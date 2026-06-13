package cn.wubo.sql.forge.cache;

import cn.wubo.sql.forge.utils.ReflectionUtils;
import org.springframework.cache.annotation.Cacheable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体元数据缓存服务，缓存类到表结构的映射关系。
 */
public class EntityCacheService {

    private static final Map<Class<?>, TableStructureInfo> metadataCache = new ConcurrentHashMap<>();

    /**
     * 构造方法，初始化实体缓存服务。
     */
    public EntityCacheService() {
    }

    /**
     * 获取实体类的表结构信息，优先从缓存读取。
     *
     * @param entityClass 实体类
     * @return 表结构信息
     */
    @Cacheable(value = "tableStructureInfo", key = "#p0")
    public TableStructureInfo getTableInfo(Class<?> entityClass) {
        return metadataCache.computeIfAbsent(entityClass, ReflectionUtils::extractTableInfo);
    }
}
