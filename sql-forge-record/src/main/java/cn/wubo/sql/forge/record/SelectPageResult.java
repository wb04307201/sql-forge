package cn.wubo.sql.forge.record;

import java.util.List;

public record SelectPageResult<T>(
        Long total,
        List<T> rows
) {
}
