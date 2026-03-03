package cn.wubo.sql.forge.record;

import cn.wubo.sql.forge.record.base.Where;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.util.List;

public record Delete(
        @JsonProperty("@where")
        @Valid
        List<Where> wheres,
        @JsonProperty("@with_select")
        @Valid
        Select select
) implements IAllowedRecord {
}