package cn.wubo.sql.forge.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 插入操作记录，描述 INSERT 语句的字段值和可选的后置查询。
 *
 * @param sets   字段名到值的映射（JSON 属性 {@code @set}），至少包含一个字段
 * @param select 插入后执行的查询（JSON 属性 {@code @with_select}），非空时返回查询结果
 */
public record Insert(
        @JsonProperty("@set")
        @NotNull
        @Size(min = 1)
        Map<String, Object> sets,
        @JsonProperty("@with_select")
        @Valid
        Select select
) implements IAllowedRecord {
}
