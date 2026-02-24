package cn.wubo.sql.forge.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

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
