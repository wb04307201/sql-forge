package cn.wubo.sql.forge.crud.base;

import cn.wubo.sql.forge.enums.JoinType;
import jakarta.validation.constraints.NotBlank;

import static cn.wubo.sql.forge.constant.Constant.*;

public record Join(
        JoinType type,
        @NotBlank
        String joinTable,
        @NotBlank
        String on
) {

    public String create() {
        return String.format(ON_TEMPLATE, joinTable, on);
    }
}
