package cn.wubo.sql.forge;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SQL Forge MCP 服务端，把后端 API 暴露为 Spring AI {@link Tool} 方法，供 AI 客户端调用。
 * <p>
 * 内部按系统名称路由到对应的 SQL Forge 实例（baseUrl 来自 {@link SqlForgeMcpProperties.SystemInfo}），
 * 所有请求统一带 {@code X-Api-Key} 鉴权头。
 * </p>
 */
public class SqlForgeMcpService {

    private final List<SqlForgeMcpProperties.SystemInfo> systems;
    private final RestClient restClient;

    /**
     * 构造 MCP 服务实例。
     *
     * @param systems    已注册系统列表（来自 {@link SqlForgeMcpProperties}）
     * @param restClient HTTP 客户端（Spring RestClient）
     */
    public SqlForgeMcpService(List<SqlForgeMcpProperties.SystemInfo> systems, RestClient restClient) {
        this.systems = systems;
        this.restClient = restClient;
    }

    // ====================== 系统/执行器解析辅助 ======================

    /**
     * 把系统名称解析为远程调用上下文，找不到时返回错误字符串。
     *
     * @param systemName 系统名称
     * @return {@link SystemContext} 或错误字符串
     */
    private Object resolveSystem(String systemName) {
        Optional<SqlForgeMcpProperties.SystemInfo> opt = systems.stream()
                .filter(s -> s.getName().equals(systemName))
                .findAny();
        if (opt.isEmpty()) {
            return "系统不存在: " + systemName;
        }
        SqlForgeMcpProperties.SystemInfo info = opt.get();
        return new SystemContext(info.getUrl(), info.getApiKey());
    }

    /**
     * 远程系统调用上下文（baseUrl + apiKey）。
     *
     * @param baseUrl 远端 SQL Forge 服务的基础 URL
     * @param apiKey  远端服务要求的 {@code X-Api-Key} 鉴权值
     */
    private record SystemContext(String baseUrl, String apiKey) {
    }

    // ====================== 系统/元数据 ======================

    @Tool(description = "获取所有系统信息")
    public String getSystems() {
        StringBuilder sb = new StringBuilder();
        sb.append("系统列表：\n");
        for (SqlForgeMcpProperties.SystemInfo system : systems) {
            sb.append(String.format("系统名称：%s 系统描述：%s\n", system.getName(), system.getDescription()));
        }
        return sb.toString();
    }

    @Tool(description = "根据系统名称，获取系统使用的数据库信息，包括数据库产品名称, 数据库产品版本")
    public String getMetaDataDatabase(@ToolParam(description = "系统名称") String systemName) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
        DatabaseInfo databaseInfo = restClient.get()
                .uri(ctx.baseUrl() + Constant.GET_METADATA_DATABASE_URL)
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .retrieve()
                .body(DatabaseInfo.class);
        return String.format("数据库产品名称：%s 数据库产品版本：%s",
                databaseInfo.productName(), databaseInfo.productVersion());
    }

    @Tool(description = "根据系统名称，获取系统使用的数据库中所有表的信息，包括catalog, schema, 表名, 表类型, 表描述")
    public String sqlForgeMetaDataTables(@ToolParam(description = "系统名称") String systemName) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
        List<TableInfo> tables = restClient.get()
                .uri(ctx.baseUrl() + Constant.GET_METADATA_TABLES_URL)
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (tables == null || tables.isEmpty()) {
            return "没有表";
        }
        StringBuilder sb = new StringBuilder("数据库表列表：\n");
        for (TableInfo table : tables) {
            sb.append(String.format("catalog: %s schema:%s 表名：%s 表类型：%s 表描述：%s\n",
                    table.tableCat(), table.tableSchema(), table.tableName(), table.tableType(), table.remarks()));
        }
        return sb.toString();
    }

    @Tool(description = "根据系统名称和表信息，获取系统数据库表结构, 包括表信息, 列信息, 主键, 外键, 索引")
    public String getMetaDataTableInfo(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "catalog", required = false) String catalog,
            @ToolParam(description = "schema", required = false) String schema,
            @ToolParam(description = "表名") String tableName,
            @ToolParam(description = "表类型", required = false) String tableType
    ) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
        URI uri = UriComponentsBuilder.fromHttpUrl(ctx.baseUrl() + Constant.GET_METADATA_TABLE_DEFINITIONS_URL)
                .queryParam("tableType", tableType)
                .queryParam("tableName", tableName)
                .queryParamIfPresent("catalog", Optional.ofNullable(catalog))
                .queryParamIfPresent("schema", Optional.ofNullable(schema))
                .build()
                .toUri();

        List<TableDefinitionInfo> tableDefinitions = restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (tableDefinitions == null || tableDefinitions.isEmpty()) {
            return "没有表结构";
        }
        StringBuilder sb = new StringBuilder();
        for (TableDefinitionInfo tableDefinition : tableDefinitions) {
            sb.append(String.format("catalog: %s schema:%s 表名：%s 表类型：%s 表描述：%s\n",
                    tableDefinition.tableCat(), tableDefinition.tableSchema(),
                    tableDefinition.tableName(), tableDefinition.tableType(), tableDefinition.remarks()));
            sb.append("列：\n");
            for (ColumnInfo column : tableDefinition.columns()) {
                sb.append(String.format("列名：%s 类型：%s 长度：%s 精度：%s 描述：%s 默认值：%s 位置：%s 可空：%s 自增：%s 生成列：%s\n",
                        column.columnName(), column.typeName(), column.columnSize(), column.decimalDigits(),
                        column.remarks(), column.columnDef(), column.ordinalPosition(),
                        column.isNullable(), column.isAutoincrement(), column.isGeneratedcolumn()));
            }
            sb.append("主键：\n");
            for (PrimaryKeyInfo primaryKey : tableDefinition.primaryKeys()) {
                sb.append(String.format("主键名：%s 主键值：%s\n", primaryKey.pkName(), primaryKey.columnName()));
            }
            sb.append("外键：\n");
            for (ForeignKeyInfo foreignKey : tableDefinition.foreignKeys()) {
                sb.append(String.format("外键名：%s 主键名：%s 主键表名：%s 主键列名：%s 外键表名：%s 外键列名：%s\n",
                        foreignKey.fkName(), foreignKey.pkName(), foreignKey.pkTableName(),
                        foreignKey.pkColumnName(), foreignKey.fkTableName(), foreignKey.fkColumnName()));
            }
            sb.append("索引：\n");
            for (IndexInfo index : tableDefinition.indexes()) {
                sb.append(String.format("索引名：%s 是否为非唯一索引：%s 索引包含的列名列表：%s 排序方向：%s\n",
                        index.indexName(), index.nonUnique(), index.columnName(), index.ascOrDesc()));
            }
        }
        return sb.toString();
    }

    @Tool(description = "根据系统名称，获取数据库的树形元数据（数据库 -> schema -> 表），比逐表查询更高效")
    public Object getMetaDataTree(@ToolParam(description = "系统名称") String systemName) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
        return restClient.get()
                .uri(ctx.baseUrl() + Constant.GET_METADATA_TREE_URL)
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .retrieve()
                .body(Object.class);
    }

    @Tool(description = "列出指定系统中所有可用的执行器名称（如 database、calcite）")
    public List<String> listExecutorNames(@ToolParam(description = "系统名称") String systemName) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return List.of(err);
        }
        SystemContext ctx = (SystemContext) resolved;
        List<String> names = restClient.get()
                .uri(ctx.baseUrl() + Constant.LIST_EXECUTOR_NAMES_URL)
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return names == null ? List.of() : names;
    }

    // ====================== SQL 执行 / 模板 ======================

    @Tool(description = "根据系统名称，执行SQL查询并返回结果集")
    public String executeSQL(@ToolParam(description = "系统名称") String systemName, @ToolParam(description = "要执行的SQL语句") String sql) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
        return restClient.post()
                .uri(ctx.baseUrl() + Constant.EXECUTE_SQL_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .body(Map.of("sql", sql))
                .retrieve()
                .body(String.class);
    }

    @Tool(description = "根据系统名称，保存页面JSON配置模板")
    public String amisTemplateSave(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id") String id,
            @ToolParam(description = "模板名称") String name,
            @ToolParam(description = "模板描述") String description,
            @ToolParam(description = "JSON配置模板") String context) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
        return restClient.put()
                .uri(ctx.baseUrl() + Constant.PUT_TEMPLATE_AMIS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .body(Map.of("id", id, "name", name, "description", description, "context", context))
                .retrieve()
                .body(String.class);
    }

    // ====================== Batch 1: JSON CRUD ======================

    /**
     * 通用 JSON CRUD 调用入口。
     *
     * @param ctx       远程系统上下文
     * @param method    CRUD 方法：select / insert / update / delete / selectPage
     * @param tableName 目标表名
     * @param body      与后端 record JSON 结构对应的请求体
     * @return 反序列化后的响应对象（由后端决定具体类型：列表、行 Map、行数、错误对象等）
     */
    private Object callJsonCrud(SystemContext ctx, String method, String tableName, Map<String, Object> body) {
        return restClient.post()
                .uri(ctx.baseUrl() + String.format(Constant.JSON_CRUD_URL, method, tableName))
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .body(body == null ? Map.of() : body)
                .retrieve()
                .body(Object.class);
    }

    @Tool(description = "JSON CRUD：通用条件查询（POST /api/json/select/{tableName}）。"
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
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        return callJsonCrud((SystemContext) resolved, "select", tableName, body);
    }

    @Tool(description = "JSON CRUD：分页查询（POST /api/json/selectPage/{tableName}）。"
            + "body 结构对应 SelectPage record：{\"@column\":[...],\"@where\":[...],"
            + "\"@page\":{\"pageIndex\":0,\"pageSize\":20},\"@join\":[...],\"@order\":[...],\"@distinct\":false}。"
            + "返回 {\"total\":Long,\"rows\":List<Map>}。")
    public Object jsonSelectPage(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "表名") String tableName,
            @ToolParam(description = "SelectPage 请求体（Map），必须包含 @page:{pageIndex,pageSize}")
            Map<String, Object> body) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        return callJsonCrud((SystemContext) resolved, "selectPage", tableName, body);
    }

    @Tool(description = "JSON CRUD：插入一条记录（POST /api/json/insert/{tableName}）。"
            + "body 结构：{\"@set\":{\"col1\":\"v1\",\"col2\":\"v2\"},\"@with_select\":null}。"
            + "可在 @with_select 中放 Select 记录以返回插入后的查询结果。")
    public Object jsonInsert(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "表名") String tableName,
            @ToolParam(description = "Insert 请求体（Map），必须包含 @set:Map<String,Object>")
            Map<String, Object> body) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        return callJsonCrud((SystemContext) resolved, "insert", tableName, body);
    }

    @Tool(description = "JSON CRUD：按条件更新记录（POST /api/json/update/{tableName}）。"
            + "body 结构：{\"@set\":{\"col\":\"newVal\"},\"@where\":[{\"column\":\"id\",\"condition\":\"EQ\",\"value\":\"x\"}],"
            + "\"@with_select\":null}。@where 缺省时更新整张表，请谨慎使用。")
    public Object jsonUpdate(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "表名") String tableName,
            @ToolParam(description = "Update 请求体（Map），必须包含 @set:Map<String,Object>，建议带 @where 限定条件")
            Map<String, Object> body) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        return callJsonCrud((SystemContext) resolved, "update", tableName, body);
    }

    @Tool(description = "JSON CRUD：按条件删除记录（POST /api/json/delete/{tableName}）。"
            + "body 结构：{\"@where\":[{\"column\":\"id\",\"condition\":\"EQ\",\"value\":\"x\"}],\"@with_select\":null}。"
            + "@where 缺省时将删除整张表，请谨慎使用。")
    public Object jsonDelete(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "表名") String tableName,
            @ToolParam(description = "Delete 请求体（Map），建议带 @where 限定条件")
            Map<String, Object> body) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        return callJsonCrud((SystemContext) resolved, "delete", tableName, body);
    }

    // ====================== Batch 2: 模板 CRUD ======================

    /**
     * 构建带 query 参数的 URI。
     *
     * @param baseUrl 基础 URL
     * @param path    相对路径
     * @param params  键值对参数（值为 null 时该参数会被忽略）
     * @return 完整 URI
     */
    private URI buildUri(String baseUrl, String path, Map<String, String> params) {
        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(baseUrl + path);
        if (params != null) {
            params.forEach((k, v) -> b.queryParamIfPresent(k, Optional.ofNullable(v)));
        }
        return b.build().toUri();
    }

    @Tool(description = "列出 Amis 模板（页面 JSON 配置）。可选按 id/name/description/context 模糊匹配")
    public Object listAmisTemplates(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id（可选）", required = false) String id,
            @ToolParam(description = "模板名称（可选，模糊匹配）", required = false) String name,
            @ToolParam(description = "模板描述（可选，模糊匹配）", required = false) String description,
            @ToolParam(description = "模板内容（可选，模糊匹配）", required = false) String context) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
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
    }

    @Tool(description = "按 id 获取单个 Amis 模板")
    public Object getAmisTemplate(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id") String id) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
        return restClient.get()
                .uri(ctx.baseUrl() + String.format(Constant.GET_TEMPLATE_AMIS_ID_URL, id))
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .retrieve()
                .body(Object.class);
    }

    @Tool(description = "按 id 删除 Amis 模板")
    public String deleteAmisTemplate(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id") String id) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
        return restClient.delete()
                .uri(ctx.baseUrl() + String.format(Constant.GET_TEMPLATE_AMIS_ID_URL, id))
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .retrieve()
                .body(String.class);
    }

    @Tool(description = "列出 SQL 模板。可选按 id/executorName/context 模糊匹配")
    public Object listSqlTemplates(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id（可选）", required = false) String id,
            @ToolParam(description = "执行器名称（可选）", required = false) String executorName,
            @ToolParam(description = "模板内容（可选，模糊匹配）", required = false) String context) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
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
    }

    @Tool(description = "按 id 获取单个 SQL 模板")
    public Object getSqlTemplate(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id") String id) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
        return restClient.get()
                .uri(ctx.baseUrl() + String.format(Constant.TEMPLATE_SQL_ID_URL, id))
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .retrieve()
                .body(Object.class);
    }

    @Tool(description = "保存或更新 SQL 模板（PUT /api/template/sql）。"
            + "如果 id 已存在则更新，否则新建。context 是带占位符的 SQL/Enjoy 模板，可使用 #{} 占位符。")
    public String saveSqlTemplate(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id") String id,
            @ToolParam(description = "模板内容（SQL/Enjoy 模板）") String context,
            @ToolParam(description = "执行器名称（可选，如 database、calcite）", required = false) String executorName,
            @ToolParam(description = "模板名称（可选）", required = false) String name,
            @ToolParam(description = "模板描述（可选）", required = false) String description) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
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
    }

    @Tool(description = "按 id 删除 SQL 模板")
    public String deleteSqlTemplate(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id") String id) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
        return restClient.delete()
                .uri(ctx.baseUrl() + String.format(Constant.TEMPLATE_SQL_ID_URL, id))
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .retrieve()
                .body(String.class);
    }

    @Tool(description = "执行 SQL 模板（POST /api/template/sql/{id}）。"
            + "params 是模板中 #{} 占位符的绑定值。推荐使用本工具代替 executeSQL 以复用后端模板并降低 SQL 注入风险。")
    public Object executeSqlTemplate(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id") String id,
            @ToolParam(description = "模板参数（Map），对应模板中 #{} 占位符的绑定值", required = false)
            Map<String, Object> params) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
        return restClient.post()
                .uri(ctx.baseUrl() + String.format(Constant.TEMPLATE_SQL_ID_URL, id))
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .body(params == null ? Map.of() : params)
                .retrieve()
                .body(Object.class);
    }

    // ====================== Batch 3: 聚合/便捷工具 ======================

    /**
     * 调用远端 metaDataTables 接口获取表清单。
     *
     * @param ctx 远程系统上下文
     * @return 表信息列表（{@code tableCat/tableSchema/tableName/tableType/remarks} 等键），后端无返回时为空列表而非 null
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchTables(SystemContext ctx) {
        List<Map<String, Object>> tables = restClient.get()
                .uri(ctx.baseUrl() + Constant.GET_METADATA_TABLES_URL)
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return tables == null ? List.of() : tables;
    }

    @Tool(description = "按关键字搜索表（不区分大小写，支持 catalog.schema.tableName 任一字段匹配）")
    public Object findTablesByName(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "关键字") String keyword) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
        List<Map<String, Object>> tables = fetchTables(ctx);
        if (keyword == null || keyword.isEmpty()) {
            return tables;
        }
        String lower = keyword.toLowerCase();
        return tables.stream()
                .filter(t -> {
                    String tn = String.valueOf(t.getOrDefault("tableName", "")).toLowerCase();
                    String sc = String.valueOf(t.getOrDefault("tableSchema", "")).toLowerCase();
                    String tc = String.valueOf(t.getOrDefault("tableCat", "")).toLowerCase();
                    String rm = String.valueOf(t.getOrDefault("remarks", "")).toLowerCase();
                    return tn.contains(lower) || sc.contains(lower) || tc.contains(lower) || rm.contains(lower);
                })
                .toList();
    }

    @Tool(description = "一键获取数据库完整 schema（数据库级 -> 表级 -> 列/主键/外键/索引）。"
            + "tableNamePattern 为空时返回所有表（大数据量库慎用，建议先用 findTablesByName 过滤）。")
    public Object describeSchema(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "表名匹配模式（SQL LIKE 语法，如 USERS、ORDER_%），为空表示全部", required = false)
            String tableNamePattern) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
        List<Map<String, Object>> tables = fetchTables(ctx);
        // 可选预过滤，减少后续 N 次 definitions 调用
        List<Map<String, Object>> targetTables;
        if (tableNamePattern == null || tableNamePattern.isEmpty()) {
            targetTables = tables;
        } else {
            String pattern = tableNamePattern.toLowerCase();
            targetTables = tables.stream()
                    .filter(t -> {
                        String tn = String.valueOf(t.getOrDefault("tableName", ""));
                        return tn.toLowerCase().contains(pattern) || pattern.equals("%") || pattern.equals("*");
                    })
                    .toList();
        }
        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Map<String, Object> t : targetTables) {
            String tn = String.valueOf(t.get("tableName"));
            String tt = String.valueOf(t.get("tableType"));
            Map<String, Object> def = restClient.get()
                    .uri(UriComponentsBuilder.fromHttpUrl(ctx.baseUrl() + Constant.GET_METADATA_TABLE_DEFINITIONS_URL)
                            .queryParam("tableName", tn)
                            .queryParam("tableType", tt)
                            .build().toUri())
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", ctx.apiKey())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("tableCat", t.get("tableCat"));
            entry.put("tableSchema", t.get("tableSchema"));
            entry.put("tableName", tn);
            entry.put("tableType", tt);
            entry.put("remarks", t.get("remarks"));
            entry.put("definition", def);
            result.add(entry);
        }
        return Map.of("count", result.size(), "tables", result);
    }

    @Tool(description = "统计指定表符合条件的记录数（等价于 SELECT count(1) FROM tableName WHERE ...）。"
            + "whereJson 结构同 Select 的 @where 数组，例如 "
            + "[{\"column\":\"status\",\"condition\":\"EQ\",\"value\":\"ACTIVE\"}]。"
            + "不传 whereJson 时统计全表行数。")
    public Object countRows(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "表名") String tableName,
            @ToolParam(description = "WHERE 条件数组（List<Map>），结构同 @where；为 null 时统计全表", required = false)
            java.util.List<Map<String, Object>> whereJson) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("@column", List.of("count(1) AS total"));
        body.put("@where", whereJson == null ? List.of() : whereJson);
        return callJsonCrud((SystemContext) resolved, "select", tableName, body);
    }

    @Tool(description = "安全执行 SQL 模板（推荐路径）：先校验模板存在、参数完整，再调用 executeSqlTemplate。"
            + "参数缺失时会返回明确错误，不会执行未绑定占位符的 SQL。")
    public Object executeSqlTemplateSafely(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id") String id,
            @ToolParam(description = "模板参数（Map）", required = false) Map<String, Object> params) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
        // 1) 校验模板存在
        Object tpl = getSqlTemplate(systemName, id);
        if (tpl instanceof String errMsg) {
            return "模板不存在: " + id + " (" + errMsg + ")";
        }
        // 2) 校验参数（如模板声明占位符名称）
        if (tpl instanceof Map<?, ?> tplMap && tplMap.get("context") instanceof String ctxStr) {
            java.util.Set<String> placeholders = new java.util.LinkedHashSet<>();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("#\\{\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*(?::[^}]*)?\\}")
                    .matcher(ctxStr);
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

    public static void main(String[] args) {
        RestClient restClient = RestClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();

        SqlForgeMcpProperties.SystemInfo systemInfo = new SqlForgeMcpProperties.SystemInfo();
        systemInfo.setName("系统名称");
        systemInfo.setUrl("http://localhost:8081");
        systemInfo.setDescription("系统描述");
        systemInfo.setApiKey("test");

        SqlForgeMcpService sqlForgeMcpService = new SqlForgeMcpService(List.of(systemInfo), restClient);

        String database = sqlForgeMcpService.getMetaDataDatabase(systemInfo.getName());
        System.out.println(database);

        String tables = sqlForgeMcpService.sqlForgeMetaDataTables(systemInfo.getName());
        System.out.println(tables);

        String tableInfo = sqlForgeMcpService.getMetaDataTableInfo(systemInfo.getName(), null, null, "USERS", "BASE TABLE");
        System.out.println(tableInfo);

        Object tree = sqlForgeMcpService.getMetaDataTree(systemInfo.getName());
        System.out.println(tree);

        List<String> names = sqlForgeMcpService.listExecutorNames(systemInfo.getName());
        System.out.println(names);
    }

}
