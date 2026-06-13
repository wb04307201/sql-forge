package cn.wubo.sql.forge.cache;

import cn.wubo.sql.forge.inter.SFunction;
import cn.wubo.sql.forge.utils.LambdaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表结构信息，包含表名、列信息及其按列名和字段名的索引映射。
 */
@Slf4j
@Data
public class TableStructureInfo {

    /**
     * 构造方法，初始化空的表结构信息。
     */
    public TableStructureInfo() {
    }

    /** 表名 */
    private String tableName;
    /** 列信息列表 */
    List<ColumnInfo> columnInfos = new ArrayList<>();
    /** 列名（小写）到列信息的映射 */
    private Map<String, ColumnInfo> columnNameColumnInfoMap = new HashMap<>();
    /** Java 字段名到列信息的映射 */
    private Map<String, ColumnInfo> fieldNameColumnInfoMap = new HashMap<>();

    /**
     * 根据 Lambda 引用获取对应的列信息。
     *
     * @param fn  字段的 Lambda 引用
     * @param <T> 实体类型
     * @return 列信息，未找到时返回 null
     */
    public <T> ColumnInfo getColumnInfo(SFunction<T, ?> fn) {
        try {
            String fieldName = LambdaUtils.getFieldName(fn);
            return fieldNameColumnInfoMap.getOrDefault(fieldName, null);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }
}
