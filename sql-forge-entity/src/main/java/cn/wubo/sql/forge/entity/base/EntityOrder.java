package cn.wubo.sql.forge.entity.base;

import cn.wubo.sql.forge.enums.OrderType;
import cn.wubo.sql.forge.inter.SFunction;

/**
 * 实体排序记录，指定排序列和排序方向。
 *
 * @param <T>       实体类型
 * @param colum     排序列的 Lambda 引用
 * @param orderType 排序方向（ASC/DESC）
 */
public record EntityOrder<T>(
        SFunction<T, ?> colum,
        OrderType orderType
) {

    /**
     * 创建升序排序。
     *
     * @param colum 排序列的 Lambda 引用
     * @return 升序排序记录
     */
    public static <T> EntityOrder<T> asc(SFunction<T, ?> colum) {
        return new EntityOrder<>(colum, OrderType.ASC);
    }

    /**
     * 创建降序排序。
     *
     * @param <T>   实体类型
     * @param colum 排序列的 Lambda 引用
     * @return 降序排序记录
     */
    public static <T> EntityOrder<T> desc(SFunction<T, ?> colum) {
        return new EntityOrder<>(colum, OrderType.DESC);
    }
}
