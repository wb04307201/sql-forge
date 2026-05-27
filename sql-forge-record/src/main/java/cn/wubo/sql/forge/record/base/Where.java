package cn.wubo.sql.forge.record.base;

import cn.wubo.sql.forge.enums.ConditionType;
import cn.wubo.sql.forge.map.ParamMap;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.stream.Collectors;

import static cn.wubo.sql.forge.constant.Constant.*;

/**
 * WHERE 条件记录，描述单个查询条件（列名 + 比较类型 + 值）。
 * <p>
 * 支持 16 种条件类型（EQ、LIKE、BETWEEN、IN 等），自动处理参数化占位符和 null 值跳过。
 * </p>
 *
 * @param column    列名
 * @param condition 条件比较类型
 * @param value     条件值
 */
public record Where(
        @NotBlank
        String column,
        ConditionType condition,
        Object value
) {

    public String create(ParamMap params) {
        if (condition == null) {
            if (value != null && !"".equals(value)) {
                params.put(value);
                return column + ConditionType.EQ.getValue() + QUESTION_MARK;
            }
            return null;
        }

        if (value == null && condition != ConditionType.IS_NULL && condition != ConditionType.IS_NOT_NULL) {
            return null;
        }

        if (value instanceof String str && str.isEmpty() && condition != ConditionType.IS_NULL && condition != ConditionType.IS_NOT_NULL) {
            return null;
        }

        if (value instanceof List<?> list && list.isEmpty() && (condition == ConditionType.IN || condition == ConditionType.NOT_IN)) {
            return null;
        }

        return switch (condition) {
            case EQ, NOT_EQ, GT, LT, GTEQ, LTEQ:
                params.put(value);
                yield column + condition.getValue() + QUESTION_MARK;
            case LIKE, NOT_LIKE:
                params.put(PERCENT + value + PERCENT);
                yield UPPER + OPERN_PAREN + column + CLOSE_PAREN + condition.getValue() + UPPER_QUESTION_MARK;
            case LEFT_LIKE:
                params.put(PERCENT + value);
                yield UPPER + OPERN_PAREN + column + CLOSE_PAREN + condition.getValue() + UPPER_QUESTION_MARK;
            case RIGHT_LIKE:
                params.put(value + PERCENT);
                yield UPPER + OPERN_PAREN + column + CLOSE_PAREN + condition.getValue() + UPPER_QUESTION_MARK;
            case BETWEEN, NOT_BETWEEN:
                if (value instanceof List<?> list && list.size() == 2) {
                    params.put(list.get(0));
                    params.put(list.get(1));
                    yield column + condition.getValue() + QUESTION_MARK + AND + QUESTION_MARK;
                } else {
                    throw new IllegalArgumentException("Invalid condition,  value must be a List");
                }
            case IN, NOT_IN:
                if (value instanceof List<?> list && !list.isEmpty()) {
                    yield column + condition.getValue() + OPERN_PAREN + getListValueStr(list, params) + CLOSE_PAREN;
                } else {
                    throw new IllegalArgumentException("Invalid condition,  value must be a List");
                }
            case IS_NULL, IS_NOT_NULL:
                yield column + condition.getValue();
        };
    }

    private String getListValueStr(Object value, ParamMap params) {
        if (value instanceof List<?> list) {
            return list.stream().map(e -> {
                params.put(e);
                return QUESTION_MARK;
            }).collect(Collectors.joining(","));
        } else {
            params.put(value);
            return QUESTION_MARK;
        }
    }

}
