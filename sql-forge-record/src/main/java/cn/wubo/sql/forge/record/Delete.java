package cn.wubo.sql.forge.record;

import cn.wubo.sql.forge.record.base.Where;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 删除操作记录，描述 DELETE 语句的条件和可选的后置查询。
 *
 * @param wheres WHERE 条件列表（JSON 属性 {@code @where}）
 * @param select 删除后执行的查询（JSON 属性 {@code @with_select}），非空时返回查询结果
 */
public record Delete(
        @JsonProperty("@where")
        @Valid
        List<Where> wheres,
        @JsonProperty("@with_select")
        @Valid
        Select select
) implements IAllowedRecord {
}