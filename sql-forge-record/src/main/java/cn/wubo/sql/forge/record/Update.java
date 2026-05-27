package cn.wubo.sql.forge.record;

import cn.wubo.sql.forge.record.base.Where;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * 更新操作记录，描述 UPDATE 语句的 SET 赋值、WHERE 条件和可选的后置查询。
 *
 * @param sets   字段名到新值的映射（JSON 属性 {@code @set}），至少包含一个字段
 * @param wheres WHERE 条件列表（JSON 属性 {@code @where}）
 * @param select 更新后执行的查询（JSON 属性 {@code @with_select}），非空时返回查询结果
 */
public record Update(
        @JsonProperty("@set")
        @NotNull
        @Size(min = 1)
        Map<String, Object> sets,
        @JsonProperty("@where")
        @Valid
        List<Where> wheres,
        @JsonProperty("@with_select")
        @Valid
        Select select
) implements IAllowedRecord {
}
