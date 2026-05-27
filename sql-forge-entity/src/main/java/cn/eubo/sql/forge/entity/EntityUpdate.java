package cn.eubo.sql.forge.entity;

import cn.eubo.sql.forge.entity.base.AbstractUpdate;
import cn.eubo.sql.forge.entity.base.EntityCondition;
import cn.eubo.sql.forge.entity.base.EntitySet;
import cn.eubo.sql.forge.cache.EntityCacheService;
import cn.eubo.sql.forge.cache.ColumnInfo;
import cn.eubo.sql.forge.cache.TableStructureInfo;
import cn.wubo.sql.forge.RecordExecutor;
import cn.wubo.sql.forge.record.Update;
import cn.wubo.sql.forge.record.base.Where;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类型安全的 UPDATE 构建器，通过 Lambda 表达式指定更新字段和条件。
 *
 * @param <T> 实体类型
 */
public class EntityUpdate<T> extends AbstractUpdate<T, Integer, EntityUpdate<T>> {

    /**
     * 构造方法。
     *
     * @param entityClass 实体类对象
     */
    public EntityUpdate(Class<T> entityClass) {
        super(entityClass);
    }

    @Override
    public Integer run(String executorName, EntityCacheService entityCacheService, RecordExecutor recordExecutor) throws Exception {
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

        List<Where> sqlWheres = new ArrayList<>();
        if (entityConditions != null && !entityConditions.isEmpty()) {
            for (EntityCondition<T> entityCondition : entityConditions) {
                ColumnInfo columnInfo = tableStructureInfo.getColumnInfo(entityCondition.column());
                if (columnInfo != null) {
                    sqlWheres.add(new Where(columnInfo.getColumnName(), entityCondition.condition(), entityCondition.value()));
                }
            }
        }

        Update update = new Update(
                sqlSets,
                sqlWheres,
                null
        );

        return (Integer) recordExecutor.update(executorName, tableStructureInfo.getTableName(), update);
    }
}
