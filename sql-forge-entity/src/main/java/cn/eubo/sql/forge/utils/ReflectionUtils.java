package cn.eubo.sql.forge.utils;

import cn.eubo.sql.forge.cache.ColumnInfo;
import cn.eubo.sql.forge.cache.TableStructureInfo;
import jakarta.persistence.*;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;

/**
 * 反射工具类，从实体类的 JPA 注解（@Table、@Column、@Id、@Lob、@Transient）中提取表结构信息。
 */
@UtilityClass
public class ReflectionUtils {

    /**
     * 从实体类中提取表结构信息（表名、列信息、主键等）。
     *
     * @param entityClass 实体类
     * @return 表结构信息
     */
    public TableStructureInfo extractTableInfo(Class<?> entityClass) {
        TableStructureInfo info = new TableStructureInfo();

        // 获取@Table注解
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null) {
            info.setTableName(tableAnnotation.name());
        } else {
            info.setTableName(StringUtils.camelToUnderscore(entityClass.getSimpleName()));
        }

        // 遍历所有字段
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Transient.class))
                continue;

            ColumnInfo columnInfo = new ColumnInfo();
            columnInfo.setField(field);
            columnInfo.setFieldName(field.getName());
            columnInfo.setJavaType(field.getType());

            // 获取@Column注解
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation != null) {
                columnInfo.setColumnName(columnAnnotation.name().isEmpty() ?
                        StringUtils.camelToUnderscore(field.getName()) : columnAnnotation.name());
                columnInfo.setNullable(columnAnnotation.nullable());
                columnInfo.setColumnDefinition(columnAnnotation.columnDefinition());
                columnInfo.setLength(columnAnnotation.length());
                columnInfo.setPrecision(columnAnnotation.precision());
                columnInfo.setScale(columnAnnotation.scale());
                columnInfo.setComment(columnAnnotation.comment());
            } else {
                columnInfo.setColumnName(StringUtils.camelToUnderscore(field.getName()));
            }

            // 检查是否为主键
            if (field.isAnnotationPresent(Id.class)) {
                columnInfo.setPrimaryKey(true);
            }

            // 检查是否为大对象
            if (field.isAnnotationPresent(Lob.class)) {
                columnInfo.setLob(true);
            }

            info.getColumnInfos().add(columnInfo);
            info.getColumnNameColumnInfoMap().put(columnInfo.getColumnName().toLowerCase(), columnInfo);
            info.getFieldNameColumnInfoMap().put(columnInfo.getFieldName(), columnInfo);
        }

        return info;
    }

}
