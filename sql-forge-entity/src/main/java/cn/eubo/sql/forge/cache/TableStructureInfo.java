package cn.eubo.sql.forge.cache;

import cn.eubo.sql.forge.inter.SFunction;
import cn.eubo.sql.forge.utils.LambdaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
public class TableStructureInfo {
    private String tableName;
    List<ColumnInfo> columnInfos = new ArrayList<>();
    private Map<String, ColumnInfo> columnNameColumnInfoMap = new HashMap<>();
    private Map<String, ColumnInfo> fieldNameColumnInfoMap = new HashMap<>();

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

