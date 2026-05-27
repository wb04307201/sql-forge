package cn.wubo.sql.forge.record.base;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 分页参数记录。
 *
 * @param pageIndex 页码（从 0 开始）
 * @param pageSize  每页条数（最小为 1）
 */
public record Page(
        @NotNull
        @Min(0)
        Integer pageIndex,
        @NotNull
        @Min(1)
        Integer pageSize
) {
}
