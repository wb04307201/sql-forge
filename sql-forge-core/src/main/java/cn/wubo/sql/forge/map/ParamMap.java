package cn.wubo.sql.forge.map;

import java.util.HashMap;

/**
 * SQL 参数映射，以 1-based 整数索引为键存储参数值。
 * <p>
 * 提供 {@link #put(Object)} 便捷方法，自动按插入顺序递增索引。
 * </p>
 */
public class ParamMap extends HashMap<Integer,Object> {

    /**
     * 以自动递增的索引添加参数值（从 1 开始）。
     *
     * @param value 参数值
     */
    public void put(Object value) {
        super.put(this.size() + 1, value);
    }
}
