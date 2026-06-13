package cn.wubo.sql.forge.entity;

import cn.wubo.sql.forge.entity.base.AbstractSelectPage;
import cn.wubo.sql.forge.entity.base.EntityCondition;
import cn.wubo.sql.forge.entity.base.EntityOrder;
import cn.wubo.sql.forge.cache.EntityCacheService;
import cn.wubo.sql.forge.cache.ColumnInfo;
import cn.wubo.sql.forge.cache.TableStructureInfo;
import cn.wubo.sql.forge.inter.SFunction;
import cn.wubo.sql.forge.utils.ValueUtils;
import cn.wubo.sql.forge.RecordExecutor;
import cn.wubo.sql.forge.record.SelectPage;
import cn.wubo.sql.forge.record.SelectPageResult;
import cn.wubo.sql.forge.record.base.Where;
import cn.wubo.sql.forge.map.RowMap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 类型安全的分页查询构建器，在 EntitySelect 基础上增加分页参数。
 *
 * @param <T> 实体类型
 */
public class EntitySelectPage<T> extends AbstractSelectPage<T, SelectPageResult<T>, EntitySelectPage<T>> {

    /**
     * 构造方法。
     *
     * @param entityClass 实体类对象
     */
    public EntitySelectPage(Class<T> entityClass) {
        super(entityClass);
    }

    @Override
    public SelectPageResult<T> run(String executorName, EntityCacheService entityCacheService, RecordExecutor recordExecutor) throws Exception {
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

        SelectPage select = new SelectPage(
                sqlColumns,
                sqlWheres,
                page,
                null,
                sqlOrders,
                distinct
        );
        SelectPageResult<RowMap> selectPageResult = recordExecutor.selectPage(executorName, tableStructureInfo.getTableName(), select);
        List<T> result = new ArrayList<>();
        for (RowMap rowMap : selectPageResult.rows()) {
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
        return new SelectPageResult<>(selectPageResult.total(), result);
    }
}
