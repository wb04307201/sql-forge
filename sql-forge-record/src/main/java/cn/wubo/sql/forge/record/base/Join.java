package cn.wubo.sql.forge.record.base;

import cn.wubo.sql.forge.enums.JoinType;
import jakarta.validation.constraints.NotBlank;

import static cn.wubo.sql.forge.Constant.ON_TEMPLATE;

/**
 * JOIN 子句记录，描述表连接类型、目标表和连接条件。
 *
 * @param type      JOIN 类型（INNER、LEFT、RIGHT、OUTER）
 * @param joinTable 被连接的表名
 * @param on        连接条件（ON 子句）
 */
public record Join(
        JoinType type,
        @NotBlank
        String joinTable,
        @NotBlank
        String on
) {

    public String create() {
        return String.format(ON_TEMPLATE, joinTable, on);
    }
}
