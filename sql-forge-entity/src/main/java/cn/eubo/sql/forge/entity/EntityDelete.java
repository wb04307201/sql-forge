package cn.eubo.sql.forge.entity;

import cn.eubo.sql.forge.entity.base.AbstractDelete;
import cn.eubo.sql.forge.entity.base.EntityCondition;
import cn.eubo.sql.forge.cache.EntityCacheService;
import cn.eubo.sql.forge.cache.ColumnInfo;
import cn.eubo.sql.forge.cache.TableStructureInfo;
import cn.wubo.sql.forge.record.Delete;
import cn.wubo.sql.forge.RecordExecutor;
import cn.wubo.sql.forge.record.base.Where;

import java.util.ArrayList;
import java.util.List;

/**
 * 类型安全的 DELETE 构建器，通过 Lambda 表达式指定删除条件。
 *
 * @param <T> 实体类型
 */
public class EntityDelete<T> extends AbstractDelete<T, Integer, EntityDelete<T>> {

    /**
     * 构造方法。
     *
     * @param entityClass 实体类对象
     */
    public EntityDelete(Class<T> entityClass) {
        super(entityClass);
    }

    @Override
    public Integer run(String executorName, EntityCacheService entityCacheService, RecordExecutor recordExecutor) throws Exception {
        TableStructureInfo tableStructureInfo = entityCacheService.getTableInfo(entityClass);

        List<Where> sqlWheres = new ArrayList<>();
        if (entityConditions != null && !entityConditions.isEmpty()) {
            for (EntityCondition<T> entityCondition : entityConditions) {
                ColumnInfo columnInfo = tableStructureInfo.getColumnInfo(entityCondition.column());
                if (columnInfo != null) {
                    sqlWheres.add(new Where(columnInfo.getColumnName(), entityCondition.condition(), entityCondition.value()));
                }
            }
        }

        Delete delete = new Delete(
                sqlWheres,
                null
        );

        return (Integer) recordExecutor.delete(executorName, tableStructureInfo.getTableName(), delete);
    }
}
