package cn.wubo.sql.forge.record;

import cn.wubo.sql.forge.record.base.Join;
import cn.wubo.sql.forge.record.base.Page;
import cn.wubo.sql.forge.record.base.Where;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 分页查询操作记录，在 {@link Select} 基础上增加分页参数。
 *
 * @param columns  查询列列表（JSON 属性 {@code @column}）
 * @param wheres   WHERE 条件列表（JSON 属性 {@code @where}）
 * @param page     分页参数（JSON 属性 {@code @page}）
 * @param joins    JOIN 列表（JSON 属性 {@code @join}）
 * @param orders   ORDER BY 列列表（JSON 属性 {@code @order}）
 * @param distinct 是否去重（JSON 属性 {@code @distince}）
 */
public record SelectPage(
        @JsonProperty("@column")
        List<String> columns,
        @JsonProperty("@where")
        @Valid
        List<Where> wheres,
        @JsonProperty("@page")
        @NotNull
        @Valid
        Page page,
        @JsonProperty("@join")
        @Valid
        List<Join> joins,
        @JsonProperty("@order")
        List<String> orders,
        @JsonProperty("@distince")
        boolean distinct
) implements IAllowedRecord {

    public Select selectCount() {
        return new Select(
                List.of("count(1) AS total"),
                wheres,
                joins,
                null,
                null,
                false
        );
    }
}
