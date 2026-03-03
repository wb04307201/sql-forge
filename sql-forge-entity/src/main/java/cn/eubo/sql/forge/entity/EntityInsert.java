package cn.eubo.sql.forge.entity;

import cn.eubo.sql.forge.entity.base.AbstractInsert;
import cn.eubo.sql.forge.entity.base.EntitySet;
import cn.eubo.sql.forge.cache.EntityCacheService;
import cn.eubo.sql.forge.cache.ColumnInfo;
import cn.eubo.sql.forge.cache.TableStructureInfo;
import cn.wubo.sql.forge.record.Insert;
import cn.wubo.sql.forge.RecordExecutor;

import java.util.HashMap;
import java.util.Map;

public class EntityInsert<T> extends AbstractInsert<T, Object, EntityInsert<T>> {

    public EntityInsert(Class<T> entityClass) {
        super(entityClass);
    }

    @Override
    public Object run(String executorName, EntityCacheService entityCacheService, RecordExecutor recordExecutor) throws Exception {
        TableStructureInfo tableStructureInfo = entityCacheService.getTableInfo(entityClass);

        Map<String, Object> sqlSets = new HashMap<>();
        if (sets != null && !sets.isEmpty()) {
            for (EntitySet<T> entitySet : sets) {
                ColumnInfo columnInfo = tableStructureInfo.getColumnInfo(entitySet.column());
                if (columnInfo != null) {
                    sqlSets.put(columnInfo.getColumnName(), entitySet.value());
                }
            }
        }

        Insert insert = new Insert(sqlSets, null);
        return recordExecutor.insert(executorName, tableStructureInfo.getTableName(), insert);
    }
}
