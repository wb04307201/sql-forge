package cn.wubo.sql.forge.record;

import cn.wubo.sql.forge.record.base.Join;
import cn.wubo.sql.forge.record.base.Page;
import cn.wubo.sql.forge.record.base.Where;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

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
