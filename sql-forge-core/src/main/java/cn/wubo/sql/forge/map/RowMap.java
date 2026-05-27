package cn.wubo.sql.forge.map;

import java.util.LinkedHashMap;

/**
 * 查询结果行映射，以列名为键、列值为值的有序 Map。
 * <p>
 * 继承 {@link LinkedHashMap} 保持列的插入顺序。
 * </p>
 */
public class RowMap extends LinkedHashMap<String,Object> {

    public RowMap(int columnCount) {
        super(columnCount);
    }
}
