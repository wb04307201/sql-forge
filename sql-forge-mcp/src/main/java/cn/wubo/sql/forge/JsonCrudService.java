package cn.wubo.sql.forge;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Map;

/**
 * JSON CRUD + 直接 SQL 执行：6 个 Tool。
 * <p>
 * 拆分自 {@link SqlForgeMcpService}。
 * </p>
 */
public class JsonCrudService {

    private final McpToolSupport support;
    private final RestClient restClient;

    public JsonCrudService(McpToolSupport support, RestClient restClient) {
        this.support = support;
        this.restClient = restClient;
    }

    private Object callJsonCrud(McpToolSupport.SystemContext ctx, String method, String tableName, Map<String, Object> body) {
        support.checkBodySize(body, "json" + Character.toUpperCase(method.charAt(0)) + method.substring(1));
        URI uri = URI.create(ctx.baseUrl() + String.format(Constant.JSON_CRUD_URL, method, McpToolSupport.encodePath(tableName)));
        return restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .body(body == null ? Map.of() : body)
                .retrieve()
                .body(Object.class);
    }

    @Tool(description = "JSON CRUD：通用条件查询（POST /sql/forge/api/json/select/{tableName}）。"
            + "body 结构对应 Select record："
            + "{\"@column\":[\"id\",\"name\"],\"@where\":[{\"column\":\"name\",\"condition\":\"EQ\",\"value\":\"foo\"}],"
            + "\"@join\":[{\"type\":\"LEFT\",\"joinTable\":\"t2\",\"on\":\"t1.id=t2.pid\"}],"
            + "\"@order\":[\"id DESC\"],\"@group\":[],\"@distinct\":false}。"
            + "condition 取值：EQ, NOT_EQ, GT, LT, GTEQ, LTEQ, LIKE, NOT_LIKE, LEFT_LIKE, RIGHT_LIKE, BETWEEN, NOT_BETWEEN, IN, NOT_IN, IS_NULL, IS_NOT_NULL")
    public Object jsonSelect(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "表名") String tableName,
            @ToolParam(description = "Select 请求体（Map），包含 @column/@where/@join/@order/@group/@distinct 键")
            Map<String, Object> body) {
        return support.withCtx(systemName, ctx -> callJsonCrud(ctx, "select", tableName, body));
    }

    @Tool(description = "JSON CRUD：分页查询（POST /sql/forge/api/json/selectPage/{tableName}）。"
            + "body 结构对应 SelectPage record：{\"@column\":[...],\"@where\":[...],"
            + "\"@page\":{\"pageIndex\":0,\"pageSize\":20},\"@join\":[...],\"@order\":[...],\"@distinct\":false}。"
            + "返回 {\"total\":Long,\"rows\":List<Map>}。")
    public Object jsonSelectPage(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "表名") String tableName,
            @ToolParam(description = "SelectPage 请求体（Map），必须包含 @page:{pageIndex,pageSize}")
            Map<String, Object> body) {
        return support.withCtx(systemName, ctx -> callJsonCrud(ctx, "selectPage", tableName, body));
    }

    @Tool(description = "JSON CRUD：插入一条记录（POST /sql/forge/api/json/insert/{tableName}）。"
            + "body 结构：{\"@set\":{\"col1\":\"v1\",\"col2\":\"v2\"},\"@with_select\":null}。"
            + "可在 @with_select 中放 Select 记录以返回插入后的查询结果。")
    public Object jsonInsert(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "表名") String tableName,
            @ToolParam(description = "Insert 请求体（Map），必须包含 @set:Map<String,Object>")
            Map<String, Object> body) {
        return support.withCtx(systemName, ctx -> callJsonCrud(ctx, "insert", tableName, body));
    }

    @Tool(description = "⚠️ DESTRUCTIVE: JSON CRUD：按条件更新记录（POST /sql/forge/api/json/update/{tableName}）。"
            + "body 结构：{\"@set\":{\"col\":\"newVal\"},\"@where\":[{\"column\":\"id\",\"condition\":\"EQ\",\"value\":\"x\"}],"
            + "\"@with_select\":null}。@where 缺省时更新整张表，AI Agent 必须主动向用户确认。")
    public Object jsonUpdate(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "表名") String tableName,
            @ToolParam(description = "Update 请求体（Map），必须包含 @set:Map<String,Object>，建议带 @where 限定条件")
            Map<String, Object> body) {
        return support.withCtx(systemName, ctx -> callJsonCrud(ctx, "update", tableName, body));
    }

    @Tool(description = "⚠️ DESTRUCTIVE: JSON CRUD：按条件删除记录（POST /sql/forge/api/json/delete/{tableName}）。"
            + "body 结构：{\"@where\":[{\"column\":\"id\",\"condition\":\"EQ\",\"value\":\"x\"}],\"@with_select\":null}。"
            + "@where 缺省时将删除整张表，AI Agent 必须主动向用户确认。")
    public Object jsonDelete(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "表名") String tableName,
            @ToolParam(description = "Delete 请求体（Map），建议带 @where 限定条件")
            Map<String, Object> body) {
        return support.withCtx(systemName, ctx -> callJsonCrud(ctx, "delete", tableName, body));
    }

    @Tool(description = "根据系统名称，执行SQL查询并返回结果集")
    public Object executeSQL(@ToolParam(description = "系统名称") String systemName,
                            @ToolParam(description = "要执行的SQL语句") String sql) {
        return support.withCtx(systemName, ctx -> restClient.post()
                .uri(ctx.baseUrl() + Constant.EXECUTE_SQL_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .body(Map.of("sql", sql))
                .retrieve()
                .body(String.class));
    }
}
