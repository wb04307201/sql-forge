package cn.eubo.sql.forge.entity;

import cn.eubo.sql.forge.entity.base.AbstractEntity;
import cn.eubo.sql.forge.cache.EntityCacheService;
import cn.eubo.sql.forge.cache.ColumnInfo;
import cn.eubo.sql.forge.cache.TableStructureInfo;
import cn.wubo.sql.forge.record.Insert;
import cn.wubo.sql.forge.RecordExecutor;
import cn.wubo.sql.forge.record.Update;
import cn.wubo.sql.forge.record.base.Where;
import cn.wubo.sql.forge.enums.ConditionType;

import java.lang.reflect.Field;
import java.util.*;

public class EntitySave<T> extends AbstractEntity<T, T, EntitySave<T>> {

    public EntitySave(Class<T> entityClass) {
        super(entityClass);
    }

    @Override
    public T run(String executorName, EntityCacheService entityCacheService, RecordExecutor recordExecutor) throws Exception {
        TableStructureInfo tableStructureInfo = entityCacheService.getTableInfo(entityClass);

        Optional<ColumnInfo> primaryKeyColumnInfoOptional = tableStructureInfo.getColumnInfos().stream().filter(ColumnInfo::isPrimaryKey).findAny();

        if (primaryKeyColumnInfoOptional.isEmpty())
            throw new IllegalArgumentException("Entity class has no primary key");

        Map<String, Object> sqlSets = new HashMap<>();
        for (ColumnInfo columnInfo : tableStructureInfo.getColumnInfos().stream().filter(columnInfo -> !columnInfo.isPrimaryKey()).toList()) {
            Field field = columnInfo.getField();
            field.setAccessible(true);
            Object value = field.get(entity);
            if (value != null)
                sqlSets.put(columnInfo.getColumnName(), value);
        }

        ColumnInfo primaryKeyColumnInfo = primaryKeyColumnInfoOptional.get();
        Field primaryKeyField = primaryKeyColumnInfo.getField();
        primaryKeyField.setAccessible(true);
        Object primaryKeyValue = primaryKeyField.get(entity);
        if (primaryKeyValue == null && primaryKeyColumnInfo.getJavaType() == String.class) {
            // insert
            String key = UUID.randomUUID().toString();
            sqlSets.put(primaryKeyColumnInfo.getColumnName(), key);
            Insert insert = new Insert(sqlSets, null);
            recordExecutor.insert(executorName, tableStructureInfo.getTableName(), insert);
            primaryKeyField.set(entity, key);
        } else if (primaryKeyValue == null) {
            // insert
            Insert insert = new Insert(sqlSets, null);
            Object key = recordExecutor.insert(executorName, tableStructureInfo.getTableName(), insert);
            if (key != null)
                primaryKeyField.set(entity, key);
        } else {
            // update
            List<Where> sqlWheres = new ArrayList<>();
            sqlWheres.add(new Where(primaryKeyColumnInfo.getColumnName(), ConditionType.EQ, primaryKeyValue));
            Update update = new Update(sqlSets, sqlWheres, null);
            recordExecutor.update(executorName, tableStructureInfo.getTableName(), update);
        }

        return entity;
    }
}
