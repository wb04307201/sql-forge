package cn.eubo.sql.forge.entity;

import cn.eubo.sql.forge.entity.base.AbstractSelect;
import cn.eubo.sql.forge.entity.base.EntityCondition;
import cn.eubo.sql.forge.entity.base.EntityOrder;
import cn.eubo.sql.forge.cache.EntityCacheService;
import cn.eubo.sql.forge.cache.ColumnInfo;
import cn.eubo.sql.forge.cache.TableStructureInfo;
import cn.eubo.sql.forge.inter.SFunction;
import cn.eubo.sql.forge.utils.ValueUtils;
import cn.wubo.sql.forge.RecordExecutor;
import cn.wubo.sql.forge.record.Select;
import cn.wubo.sql.forge.record.base.Where;
import cn.wubo.sql.forge.map.RowMap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 类型安全的 SELECT 构建器，通过 Lambda 表达式指定查询列和条件。
 *
 * @param <T> 实体类型
 */
public class EntitySelect<T> extends AbstractSelect<T, List<T>, EntitySelect<T>> {

    /**
     * 构造方法。
     *
     * @param entityClass 实体类对象
     */
    public EntitySelect(Class<T> entityClass) {
        super(entityClass);
    }

    @Override
    public List<T> run(String executorName, EntityCacheService entityCacheService, RecordExecutor recordExecutor) throws Exception {
        TableStructureInfo tableStructureInfo = entityCacheService.getTableInfo(entityClass);

        List<String> sqlColumns = new ArrayList<>();
        if (columns != null && !columns.isEmpty())
            for (SFunction<T, ?> column : columns) {
                ColumnInfo columnInfo = tableStructureInfo.getColumnInfo(column);
                if (columnInfo != null)
                    sqlColumns.add(columnInfo.getColumnName());
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

        List<String> sqlOrders = new ArrayList<>();
        if (orders != null && !orders.isEmpty()) {
            for (EntityOrder<T> entityOrder : orders) {
                ColumnInfo columnInfo = tableStructureInfo.getColumnInfo(entityOrder.colum());
                if (columnInfo != null) {
                    sqlOrders.add(columnInfo.getColumnName() + entityOrder.orderType().getValue());
                }
            }
        }

        Select select = new Select(
                sqlColumns,
                sqlWheres,
                null,
                sqlOrders,
                null,
                distinct
        );
        List<RowMap> list = recordExecutor.select(executorName, tableStructureInfo.getTableName(), select);
        List<T> result = new ArrayList<>();
        for (RowMap rowMap : list) {
            T obj = entityClass.getDeclaredConstructor().newInstance();
            for (String key : rowMap.keySet()) {
                ColumnInfo columnInfo = tableStructureInfo.getColumnNameColumnInfoMap().getOrDefault(key.toLowerCase(), null);
                if (columnInfo != null) {
                    Field field = columnInfo.getField();
                    field.setAccessible(true);

                    Object value = rowMap.get(key);
                    if (value != null) {
                        Object convertedValue = ValueUtils.convertValueToFieldType(value, columnInfo.getJavaType());
                        field.set(obj, convertedValue);
                    }
                }
            }
            result.add(obj);
        }
        return result;
    }
}
