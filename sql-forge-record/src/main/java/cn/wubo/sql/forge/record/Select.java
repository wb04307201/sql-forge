package cn.wubo.sql.forge.record;

import cn.wubo.sql.forge.record.base.Join;
import cn.wubo.sql.forge.record.base.Where;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 查询操作记录，描述 SELECT 语句的列、条件、连接、排序和分组。
 *
 * @param columns  查询列列表（JSON 属性 {@code @column}）
 * @param wheres   WHERE 条件列表（JSON 属性 {@code @where}）
 * @param joins    JOIN 列表（JSON 属性 {@code @join}）
 * @param orders   ORDER BY 列列表（JSON 属性 {@code @order}）
 * @param groups   GROUP BY 列列表（JSON 属性 {@code @group}）
 * @param distinct 是否去重（JSON 属性 {@code @distinct}）
 */
public record Select(
        @JsonProperty("@column")
        List<String> columns,
        @JsonProperty("@where")
        @Valid
        List<Where> wheres,
        @JsonProperty("@join")
        @Valid
        List<Join> joins,
        @JsonProperty("@order")
        List<String> orders,
        @JsonProperty("@group")
        List<String> groups,
        @JsonProperty("@distinct")
        boolean distinct
) implements IAllowedRecord {
}
