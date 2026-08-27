package cn.wubo.sql.forge.amis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Amis 模板静态校验器：在不启动浏览器的前提下检查 JSON Schema 的常见错误。
 *
 * 校验维度：
 * <ul>
 *   <li>JSON 语法解析</li>
 *   <li>必填字段（参考 catalog 中的 required 字段）</li>
 *   <li>组件 type 拼写（必须在 catalog 中存在或属于内置原子组件）</li>
 *   <li>api 字段格式（必须以 HTTP method 开头）</li>
 *   <li>嵌套组件递归检查</li>
 * </ul>
 *
 * 校验结果通过 {@link ValidationResult} 返回，包含错误列表和严重等级。
 */
public class AmisValidator {

    private static final Pattern API_PATTERN = Pattern.compile("^(GET|POST|PUT|DELETE|HEAD|PATCH)\\s+\\S+",
            Pattern.CASE_INSENSITIVE);

    /**
     * 需要按"单对象或数组"递归的容器键（值是单个组件对象或组件对象数组）。
     */
    private static final List<String> CHILD_KEYS = List.of("body", "columns", "tabs", "steps");

    /**
     * 值为单个组件对象的键（如 dialog / drawer 内部是 form）。
     */
    private static final List<String> SINGLE_OBJECT_KEYS = List.of("dialog", "drawer");

    /**
     * 值为组件对象数组的工具栏键（headerToolbar / footerToolbar）。
     */
    private static final List<String> TOOLBAR_KEYS = List.of("headerToolbar", "footerToolbar");

    /**
     * 内置原子组件：不在 catalog 中但 amis 内置可识别。
     * 这些组件是简单的展示元素（如 text、tpl、divider 等），不强制要求 props。
     */
    private static final List<String> BUILTIN_TYPES = List.of(
            "text", "tpl", "html", "plain", "remark",
            "divider", "hr", "link", "image", "audio", "video", "iframe",
            "mapping", "each", "condition", "wrap", "group",
            "hidden", "spinner", "progress", "status",
            "operation", "action", "badge", "icon",
            "anchor-nav", "breadcrumb", "nav",
            "combo", "input-group", "input-tree", "input-city",
            "input-file", "input-image", "input-rich-text",
            "input-tag", "input-color", "input-excel",
            "input-repeat", "input-month", "input-quarter",
            "input-range", "input-rating", "input-time",
            "input-kv", "input-table", "input-tree-select",
            "input-year", "search-box", "list",
            "static", "stepresult", "service", "steps",
            "json", "code", "markdown", "office",
            "pagination", "timeline", "property",
            "carousel", "collapse", "dropdown",
            "tooltip-wrapper", "portlet", "tasks",
            "tag", "diff", "log", "formula",
            "card", "card-group", "card-2", "card-3",
            // 列内容类型（嵌套在 columns[].type）
            "date", "datetime", "time", "progress", "image", "audio", "video",
            "link", "tpl", "text", "mapping", "tag", "json"
    );

    private final AmisKnowledgeService knowledge;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造校验器。
     *
     * @param knowledge 知识服务（用于查 component 必填字段）
     */
    public AmisValidator(AmisKnowledgeService knowledge) {
        this.knowledge = knowledge;
    }

    /**
     * 校验一段 Amis 模板 JSON。
     *
     * @param context 模板 JSON 字符串
     * @return 校验结果（包含 valid 标志 + 错误列表）
     */
    public ValidationResult validate(String context) {
        List<ValidationError> errors = new ArrayList<>();
        if (context == null || context.isBlank()) {
            errors.add(new ValidationError("error", "模板内容为空", "$"));
            return new ValidationResult(false, errors);
        }
        Object parsed;
        try {
            parsed = objectMapper.readValue(context, Object.class);
        } catch (Exception ex) {
            errors.add(new ValidationError("error", "JSON 解析失败: " + ex.getMessage(), "$"));
            return new ValidationResult(false, errors);
        }
        if (!(parsed instanceof Map<?, ?> root)) {
            errors.add(new ValidationError("error", "Schema 根节点必须是对象", "$"));
            return new ValidationResult(false, errors);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> rootMap = (Map<String, Object>) root;
        walk(rootMap, "$", errors);
        // valid = 无 error 级问题（warning 不影响）
        boolean hasError = errors.stream().anyMatch(e -> "error".equals(e.severity()));
        return new ValidationResult(!hasError, errors);
    }

    /**
     * 递归遍历节点、检查必填字段 / api 格式 / type 拼写。
     *
     * @param node   当前节点
     * @param path   当前节点路径（如 {@code $.body.columns[0]}）
     * @param errors 错误列表（就地追加）
     */
    private void walk(Map<String, Object> node, String path, List<ValidationError> errors) {
        String type = stringOf(node.get("type"));
        validateTypeAndRequired(type, node, path, errors);
        validateApiFormat(node, path, errors);
        // chart.config 是 G2Plot 配置对象，子 type (line/bar/pie) 是图表类型不是 amis 组件 type，
        // 跳过对 chart.config 的递归避免误报
        if ("chart".equals(type) && node.get("config") instanceof Map<?, ?>) {
            return;
        }
        // 递归 body / columns / tabs / steps（值是单对象或数组）
        for (String key : CHILD_KEYS) {
            recurseChild(node.get(key), key, path, errors, false);
        }
        // 递归 dialog / drawer（值是单对象）
        for (String key : SINGLE_OBJECT_KEYS) {
            recurseChild(node.get(key), key, path, errors, true);
        }
        // 递归 headerToolbar / footerToolbar（值是数组）
        for (String key : TOOLBAR_KEYS) {
            recurseChild(node.get(key), key, path, errors, false);
        }
    }

    /**
     * 校验 type 拼写与必填字段。
     *
     * @param type   节点的 type（可为 null）
     * @param node   当前节点
     * @param path   当前节点路径
     * @param errors 错误列表
     */
    private void validateTypeAndRequired(String type, Map<String, Object> node, String path, List<ValidationError> errors) {
        if (type == null) {
            return;
        }
        AmisKnowledgeService.ComponentSpec spec = knowledge.getComponentSpec(type);
        if (spec == null && !BUILTIN_TYPES.contains(type)) {
            errors.add(new ValidationError("warning", "未知组件 type: " + type, path + ".type"));
            return;
        }
        if (spec != null && spec.required() != null) {
            for (String req : spec.required()) {
                if (!node.containsKey(req)) {
                    errors.add(new ValidationError("error", "缺少必填字段: " + req, path + "." + req));
                }
            }
        }
    }

    /**
     * 校验 api 字符串是否符合 {@code METHOD /path} 格式。
     */
    private void validateApiFormat(Map<String, Object> node, String path, List<ValidationError> errors) {
        Object api = node.get("api");
        if (api instanceof String apiStr && !API_PATTERN.matcher(apiStr.trim()).matches()) {
            errors.add(new ValidationError("warning",
                    "api 格式不规范，应以 HTTP method 开头（如 'POST /path'）: " + apiStr,
                    path + ".api"));
        }
    }

    /**
     * 递归子节点：支持单对象、对象数组两种形式。
     *
     * @param value        子节点值（可为 null / Map / List）
     * @param key          子节点在父节点中的键名
     * @param parentPath   父节点路径
     * @param errors       错误列表
     * @param singleObject 是否仅接受单对象（dialog / drawer 场景）
     */
    private void recurseChild(Object value, String key, String parentPath, List<ValidationError> errors, boolean singleObject) {
        if (value instanceof Map<?, ?> child) {
            @SuppressWarnings("unchecked")
            Map<String, Object> childMap = (Map<String, Object>) child;
            walk(childMap, parentPath + "." + key, errors);
        } else if (!singleObject && value instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item instanceof Map<?, ?> child) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> childMap = (Map<String, Object>) child;
                    walk(childMap, parentPath + "." + key + "[" + i + "]", errors);
                }
            }
        }
    }

    private static String stringOf(Object o) {
        return o instanceof String s ? s : null;
    }

    /**
     * 校验结果。
     *
     * @param valid  true = 无 error 级问题
     * @param errors 错误列表（warning 不影响 valid）
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ValidationResult(boolean valid, List<ValidationError> errors) {
    }

    /**
     * 单条校验错误。
     *
     * @param severity 严重等级：error / warning
     * @param message  错误描述
     * @param path     节点路径（如 {@code $.body.api}）
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ValidationError(String severity, String message, String path) {
    }
}