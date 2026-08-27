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
 * 元数据查询：8 个 Tool —— 数据库信息 / 表清单 / 表结构 / 执行器列表 / 搜索 / 统计 / 描述。
 * <p>
 * 拆分自 {@link SqlForgeMcpService}（原 700 行单类）以提升可维护性。
 * </p>
 */
public class MetadataService {

    private final McpToolSupport support;
    private final RestClient restClient;

    public MetadataService(McpToolSupport support, RestClient restClient) {
        this.support = support;
        this.restClient = restClient;
    }

    @Tool(description = "根据系统名称，获取系统使用的数据库信息，包括数据库产品名称, 数据库产品版本")
    public Object getMetaDataDatabase(@ToolParam(description = "系统名称") String systemName) {
        return support.withCtx(systemName, ctx -> {
            DatabaseInfo info = restClient.get()
                    .uri(ctx.baseUrl() + Constant.GET_METADATA_DATABASE_URL)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", ctx.apiKey())
                    .retrieve()
                    .body(DatabaseInfo.class);
            return String.format("数据库产品名称：%s 数据库产品版本：%s",
                    info.productName(), info.productVersion());
        });
    }

    @Tool(description = "根据系统名称，获取系统使用的数据库中所有表的信息，包括catalog, schema, 表名, 表类型, 表描述")
    public Object sqlForgeMetaDataTables(@ToolParam(description = "系统名称") String systemName) {
        return support.withCtx(systemName, ctx -> {
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
        });
    }

    @Tool(description = "根据系统名称和表信息，获取系统数据库表结构, 包括表信息, 列信息, 主键, 外键, 索引")
    public Object getMetaDataTableInfo(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "catalog", required = false) String catalog,
            @ToolParam(description = "schema", required = false) String schema,
            @ToolParam(description = "表名") String tableName,
            @ToolParam(description = "表类型", required = false) String tableType
    ) {
        return support.withCtx(systemName, ctx -> {
            URI uri = UriComponentsBuilder.fromHttpUrl(ctx.baseUrl() + Constant.GET_METADATA_TABLE_DEFINITIONS_URL)
                    .queryParamIfPresent("tableType", Optional.ofNullable(tableType).filter(s -> !s.isBlank()))
                    .queryParam("tableName", tableName)
                    .queryParamIfPresent("catalog", Optional.ofNullable(catalog).filter(s -> !s.isBlank()))
                    .queryParamIfPresent("schema", Optional.ofNullable(schema).filter(s -> !s.isBlank()))
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
        });
    }

    @Tool(description = "根据系统名称，获取数据库的树形元数据（数据库 -> schema -> 表），比逐表查询更高效")
    public Object getMetaDataTree(@ToolParam(description = "系统名称") String systemName) {
        return support.withCtx(systemName, ctx -> restClient.get()
                .uri(ctx.baseUrl() + Constant.GET_METADATA_TREE_URL)
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", ctx.apiKey())
                .retrieve()
                .body(Object.class));
    }

    @Tool(description = "列出指定系统中所有可用的执行器名称（如 database、calcite）")
    public Object listExecutorNames(@ToolParam(description = "系统名称") String systemName) {
        return support.withCtx(systemName, ctx -> {
            List<String> names = restClient.get()
                    .uri(ctx.baseUrl() + Constant.LIST_EXECUTOR_NAMES_URL)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", ctx.apiKey())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return names == null ? List.of() : names;
        });
    }

    @Tool(description = "按关键字搜索表（不区分大小写，支持 catalog.schema.tableName 任一字段匹配）")
    public Object findTablesByName(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "关键字") String keyword) {
        return support.withCtx(systemName, ctx -> {
            List<Map<String, Object>> tables = restClient.get()
                    .uri(ctx.baseUrl() + Constant.GET_METADATA_TABLES_URL)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", ctx.apiKey())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
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
        });
    }

    @Tool(description = "一键获取数据库完整 schema（数据库级 -> 表级 -> 列/主键/外键/索引）。"
            + "tableNamePattern 为空时返回所有表（大数据量库慎用，建议先用 findTablesByName 过滤）。")
    public Object describeSchema(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "表名匹配模式（SQL LIKE 语法，如 USERS、ORDER_%），为空表示全部", required = false)
            String tableNamePattern) {
        return support.withCtx(systemName, ctx -> {
            List<Map<String, Object>> tables = restClient.get()
                    .uri(ctx.baseUrl() + Constant.GET_METADATA_TABLES_URL)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", ctx.apiKey())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
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
                List<Map<String, Object>> def = restClient.get()
                        .uri(UriComponentsBuilder.fromHttpUrl(ctx.baseUrl() + Constant.GET_METADATA_TABLE_DEFINITIONS_URL)
                                .queryParam("tableName", tn)
                                .queryParamIfPresent("tableType", Optional.ofNullable(tt).filter(s -> !s.isBlank()))
                                .build().toUri())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Api-Key", ctx.apiKey())
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
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
            Map<String, Object> out = new java.util.LinkedHashMap<>();
            out.put("count", result.size());
            out.put("tables", result);
            return out;
        });
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
        return support.withCtx(systemName, ctx -> {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("@column", List.of("count(1) AS total"));
            body.put("@where", whereJson == null ? List.of() : whereJson);
            URI uri = URI.create(ctx.baseUrl()
                    + String.format(Constant.JSON_CRUD_URL, "select", McpToolSupport.encodePath(tableName)));
            return restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", ctx.apiKey())
                    .body(body)
                    .retrieve()
                    .body(Object.class);
        });
    }
}
