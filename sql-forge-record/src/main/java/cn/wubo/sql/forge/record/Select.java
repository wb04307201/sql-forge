package cn.wubo.sql.forge.record;

import cn.wubo.sql.forge.record.base.Join;
import cn.wubo.sql.forge.record.base.Where;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.util.List;

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
        @JsonProperty("@distince")
        boolean distinct
) implements IAllowedRecord {
}
