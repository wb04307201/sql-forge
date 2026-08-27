package cn.wubo.sql.forge;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板 CRUD + SQL 模板执行：10 个 Tool。
 * <p>
 * 拆分自 {@link SqlForgeMcpService}。
 * </p>
 */
public class TemplateService {

    /** 模板占位符正则：#{name} 或 #{name:default} */
    private static final Pattern PLACEHOLDER = Pattern.compile("#\\{\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*(?::[^}]*)?\\}");

    private final McpToolSupport support;
    private final RestClient restClient;

    public TemplateService(McpToolSupport support, RestClient restClient) {
        this.support = support;
        this.restClient = restClient;
    }

    // ====================== Amis 模板 ======================

    @Tool(description = "列出 Amis 模板（页面 JSON 配置）。可选按 id/name/description/context 模糊匹配")
    public Object listAmisTemplates(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id（可选）", required = false) String id,
            @ToolParam(description = "模板名称（可选，模糊匹配）", required = false) String name,
            @ToolParam(description = "模板描述（可选，模糊匹配）", required = false) String description,
            @ToolParam(description = "模板内容（可选，模糊匹配）", required = false) String context) {
        return support.withCtx(systemName, ctx -> {
            URI uri = buildUri(ctx.baseUrl(), Constant.GET_TEMPLATE_AMIS_URL,
                    new java.util.LinkedHashMap<>(Map.of(
                            "id", id == null ? "" : id,
                            "name", name == null ? "" : name,
                            "description", description == null ? "" : description,
                            "context", context == null ? "" : context)));
            return restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", ctx.apiKey())
                    .retrieve()
                    .body(Object.class);
        });
    }

    @Tool(description = "按 id 获取单个 Amis 模板")
    public Object getAmisTemplate(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id") String id) {
        return support.withCtx(systemName, ctx -> {
            URI uri = URI.create(ctx.baseUrl() + String.format(Constant.GET_TEMPLATE_AMIS_ID_URL, McpToolSupport.encodePath(id)));
            return restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", ctx.apiKey())
                    .retrieve()
                    .body(Object.class);
        });
    }

    @Tool(description = "根据系统名称，保存页面JSON配置模板")
    public Object amisTemplateSave(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id") String id,
            @ToolParam(description = "模板名称") String name,
            @ToolParam(description = "模板描述") String description,
            @ToolParam(description = "JSON配置模板") String context) {
        return support.withCtx(systemName, ctx -> restClient.put()
                .uri(ctx.baseUrl() + Constant.PUT_TEMPLATE_AMIS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .body(Map.of("id", id, "name", name, "description", description, "context", context))
                .retrieve()
                .body(String.class));
    }

    @Tool(description = "⚠️ DESTRUCTIVE: 按 id 删除 Amis 模板（AI Agent 必须主动向用户确认）")
    public Object deleteAmisTemplate(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id") String id) {
        return support.withCtx(systemName, ctx -> {
            URI uri = URI.create(ctx.baseUrl() + String.format(Constant.GET_TEMPLATE_AMIS_ID_URL, McpToolSupport.encodePath(id)));
            return restClient.delete()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", ctx.apiKey())
                    .retrieve()
                    .body(String.class);
        });
    }

    // ====================== SQL 模板 ======================

    @Tool(description = "列出 SQL 模板。可选按 id/executorName/context 模糊匹配")
    public Object listSqlTemplates(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id（可选）", required = false) String id,
            @ToolParam(description = "执行器名称（可选）", required = false) String executorName,
            @ToolParam(description = "模板内容（可选，模糊匹配）", required = false) String context) {
        return support.withCtx(systemName, ctx -> {
            URI uri = buildUri(ctx.baseUrl(), Constant.TEMPLATE_SQL_URL,
                    new java.util.LinkedHashMap<>(Map.of(
                            "id", id == null ? "" : id,
                            "executorName", executorName == null ? "" : executorName,
                            "context", context == null ? "" : context)));
            return restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", ctx.apiKey())
                    .retrieve()
                    .body(Object.class);
        });
    }

    @Tool(description = "按 id 获取单个 SQL 模板")
    public Object getSqlTemplate(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id") String id) {
        return support.withCtx(systemName, ctx -> {
            URI uri = URI.create(ctx.baseUrl() + String.format(Constant.TEMPLATE_SQL_ID_URL, McpToolSupport.encodePath(id)));
            return restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", ctx.apiKey())
                    .retrieve()
                    .body(Object.class);
        });
    }

    @Tool(description = "保存或更新 SQL 模板（PUT /api/template/sql）。"
            + "如果 id 已存在则更新，否则新建。context 是带占位符的 SQL/Enjoy 模板，可使用 #{} 占位符。")
    public Object saveSqlTemplate(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id") String id,
            @ToolParam(description = "模板内容（SQL/Enjoy 模板）") String context,
            @ToolParam(description = "执行器名称（可选，如 database、calcite）", required = false) String executorName,
            @ToolParam(description = "模板名称（可选）", required = false) String name,
            @ToolParam(description = "模板描述（可选）", required = false) String description) {
        return support.withCtx(systemName, ctx -> {
            java.util.LinkedHashMap<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("id", id);
            body.put("context", context);
            if (executorName != null) body.put("executorName", executorName);
            if (name != null) body.put("name", name);
            if (description != null) body.put("description", description);
            String result = restClient.put()
                    .uri(ctx.baseUrl() + Constant.TEMPLATE_SQL_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", ctx.apiKey())
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return "保存成功: " + id + (result == null ? "" : " (" + result + ")");
        });
    }

    @Tool(description = "⚠️ DESTRUCTIVE: 按 id 删除 SQL 模板（AI Agent 必须主动向用户确认）")
    public Object deleteSqlTemplate(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id") String id) {
        return support.withCtx(systemName, ctx -> {
            URI uri = URI.create(ctx.baseUrl() + String.format(Constant.TEMPLATE_SQL_ID_URL, McpToolSupport.encodePath(id)));
            return restClient.delete()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", ctx.apiKey())
                    .retrieve()
                    .body(String.class);
        });
    }

    @Tool(description = "执行 SQL 模板（POST /api/template/sql/{id}）。"
            + "params 是模板中 #{} 占位符的绑定值。推荐使用本工具代替 executeSQL 以复用后端模板并降低 SQL 注入风险。")
    public Object executeSqlTemplate(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id") String id,
            @ToolParam(description = "模板参数（Map），对应模板中 #{} 占位符的绑定值", required = false)
            Map<String, Object> params) {
        return support.withCtx(systemName, ctx -> {
            URI uri = URI.create(ctx.baseUrl() + String.format(Constant.TEMPLATE_SQL_ID_URL, McpToolSupport.encodePath(id)));
            return restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", ctx.apiKey())
                    .body(params == null ? Map.of() : params)
                    .retrieve()
                    .body(Object.class);
        });
    }

    @Tool(description = "安全执行 SQL 模板（推荐路径）：先校验模板存在、参数完整，再调用 executeSqlTemplate。"
            + "参数缺失时会返回明确错误，不会执行未绑定占位符的 SQL。")
    public Object executeSqlTemplateSafely(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id") String id,
            @ToolParam(description = "模板参数（Map）", required = false) Map<String, Object> params) {
        // 0) 校验系统存在（早失败）
        Object resolved = support.resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        // 1) 校验模板存在
        Object tpl = getSqlTemplate(systemName, id);
        if (tpl instanceof String errMsg) {
            return "模板不存在: " + id + " (" + errMsg + ")";
        }
        // 2) 校验参数
        if (tpl instanceof Map<?, ?> tplMap && tplMap.get("context") instanceof String ctxStr) {
            java.util.Set<String> placeholders = new java.util.LinkedHashSet<>();
            Matcher m = PLACEHOLDER.matcher(ctxStr);
            while (m.find()) {
                placeholders.add(m.group(1));
            }
            java.util.Set<String> missing = new java.util.LinkedHashSet<>();
            for (String p : placeholders) {
                if (params == null || !params.containsKey(p)) {
                    missing.add(p);
                }
            }
            if (!missing.isEmpty()) {
                return "参数缺失: " + missing + "，模板声明的占位符: " + placeholders;
            }
        }
        // 3) 执行
        return executeSqlTemplate(systemName, id, params);
    }

    /**
     * 构建带 query 参数的 URI。
     */
    private URI buildUri(String baseUrl, String path, Map<String, String> params) {
        org.springframework.web.util.UriComponentsBuilder b =
                org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(baseUrl + path);
        if (params != null) {
            params.forEach((k, v) -> b.queryParamIfPresent(k,
                    java.util.Optional.ofNullable(v).filter(s -> !s.isEmpty())));
        }
        return b.build().toUri();
    }
}
