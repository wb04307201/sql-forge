package cn.eubo.sql.forge.entity;

import cn.eubo.sql.forge.entity.base.AbstractEntity;
import cn.eubo.sql.forge.cache.EntityCacheService;
import cn.eubo.sql.forge.cache.ColumnInfo;
import cn.eubo.sql.forge.cache.TableStructureInfo;
import cn.wubo.sql.forge.record.Delete;
import cn.wubo.sql.forge.RecordExecutor;
import cn.wubo.sql.forge.record.base.Where;
import cn.wubo.sql.forge.enums.ConditionType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EntityDeleteByKey<T> extends AbstractEntity<T, Integer, EntityDeleteByKey<T>> {

    public EntityDeleteByKey(Class<T> entityClass) {
        super(entityClass);
    }

    @Override
    public Integer run(String executorName, EntityCacheService entityCacheService, RecordExecutor recordExecutor) throws Exception {
        TableStructureInfo tableStructureInfo = entityCacheService.getTableInfo(entityClass);

        Optional<ColumnInfo> primaryKeyColumnInfoOptional = tableStructureInfo.getColumnInfos().stream().filter(ColumnInfo::isPrimaryKey).findAny();

        if (primaryKeyColumnInfoOptional.isEmpty())
            throw new IllegalArgumentException("Entity class has no primary key");

        ColumnInfo primaryKeyColumnInfo = primaryKeyColumnInfoOptional.get();
        Field primaryKeyField = primaryKeyColumnInfo.getField();
        primaryKeyField.setAccessible(true);
        Object primaryKeyValue = primaryKeyField.get(entity);
        if (primaryKeyValue == null) {
            throw new IllegalArgumentException("Entity class primary key value is null");
        }

        List<Where> sqlWheres = new ArrayList<>();
        sqlWheres.add(new Where(primaryKeyColumnInfo.getColumnName(), ConditionType.EQ, primaryKeyValue));
        Delete delete = new Delete(sqlWheres, null);

        return (Integer) recordExecutor.delete(executorName, tableStructureInfo.getTableName(), delete);
    }
}
