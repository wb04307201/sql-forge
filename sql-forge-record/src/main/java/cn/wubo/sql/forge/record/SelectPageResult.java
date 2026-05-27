package cn.wubo.sql.forge.record;

import java.util.List;

/**
 * 分页查询结果，包含总记录数和当前页数据。
 *
 * @param <T>   行数据类型
 * @param total 总记录数
 * @param rows  当前页数据列表
 */
public record SelectPageResult<T>(
        Long total,
        List<T> rows
) {
}
