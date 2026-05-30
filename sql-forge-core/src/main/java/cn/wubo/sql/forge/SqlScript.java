package cn.wubo.sql.forge;

import cn.wubo.sql.forge.map.ParamMap;
import jakarta.validation.constraints.NotBlank;

/**
 * SQL 脚本记录，封装参数化 SQL 语句及其绑定参数。
 *
 * @param sql    参数化 SQL 语句（使用 {@code ?} 占位符）
 * @param params 按索引绑定的参数映射
 */
public record SqlScript(
        @NotBlank
        String sql,
        ParamMap params
) {
}
