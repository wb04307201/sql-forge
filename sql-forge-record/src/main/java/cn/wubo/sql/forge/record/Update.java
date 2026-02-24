package cn.wubo.sql.forge.record;

import cn.wubo.sql.forge.record.base.Where;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

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
