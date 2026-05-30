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

public class SqlForgeMcpService {

    private final List<SqlForgeMcpProperties.SystemInfo> systems;
    private final RestClient restClient;

    public SqlForgeMcpService(List<SqlForgeMcpProperties.SystemInfo> systems, RestClient restClient) {
        this.systems = systems;
        this.restClient = restClient;
    }

    @Tool(description = "获取所有系统信息")
    private String getSystems() {
        StringBuilder sb = new StringBuilder();
        sb.append("系统列表：\n");
        for (SqlForgeMcpProperties.SystemInfo system : systems) {
            sb.append(String.format("系统名称：%s 系统描述：%s", system.getName(), system.getDescription()));
        }
        return sb.toString();
    }

    @Tool(description = "根据系统名称，获取系统使用的数据库信息，包括数据库产品名称, 数据库产品版本")
    private String getMetaDataDatabase(@ToolParam(description = "系统名称") String systemName) {
        Optional<SqlForgeMcpProperties.SystemInfo> optionalSystem = systems.stream()
                .filter(system -> system.getName().equals(systemName))
                .findAny();
        if (optionalSystem.isPresent()) {
            DatabaseInfo databaseInfo = restClient.get()
                    .uri(optionalSystem.get().getUrl() + Constant.GET_METADATA_DATABASE_URL)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key",optionalSystem.get().getApiKey())
                    .retrieve()
                    .body(DatabaseInfo.class);
            return String.format("数据库产品名称：%s 数据库产品版本：%s", databaseInfo.productName(), databaseInfo.productVersion());
        } else {
            return "系统不存在";
        }
    }

    @Tool(description = "根据系统名称，获取系统使用的数据库中所有表的信息，包括catalog, schema, 表名, 表类型, 表描述")
    private String sqlForgeMetaDataTables(@ToolParam(description = "系统名称") String systemName) {
        Optional<SqlForgeMcpProperties.SystemInfo> optionalSystem = systems.stream()
                .filter(system -> system.getName().equals(systemName))
                .findAny();
        if (optionalSystem.isPresent()) {
            List<TableInfo> tables = restClient.get()
                    .uri(optionalSystem.get().getUrl() + Constant.GET_METADATA_TABLES_URL)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key",optionalSystem.get().getApiKey())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (tables == null || tables.isEmpty()) {
                return "没有表";
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("数据库表列表：\n");
                for (TableInfo table : tables) {
                    sb.append(String.format("catalog: %s schema:%s 表名：%s 表类型：%s 表描述：%s\n", table.tableCat(), table.tableSchema(), table.tableName(), table.tableType(), table.remarks()));
                }
                return sb.toString();
            }
        } else {
            return "系统不存在";
        }
    }

    @Tool(description = "根据系统名称和表信息，获取系统数据库表结构, 包括表信息, 列信息, 主键, 外键, 索引")
    private String getMetaDataTableInfo(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "catalog", required = false) String catalog,
            @ToolParam(description = "schema", required = false) String schema,
            @ToolParam(description = "表名") String tableName,
            @ToolParam(description = "表类型", required = false) String tableType
    ) {
        Optional<SqlForgeMcpProperties.SystemInfo> optionalSystem = systems.stream()
                .filter(system -> system.getName().equals(systemName))
                .findAny();
        if (optionalSystem.isPresent()) {
            URI uri = UriComponentsBuilder.fromHttpUrl(optionalSystem.get().getUrl() + Constant.GET_METADATA_TABLE_DEFINITIONS_URL)
                    .queryParam("tableType", tableType)
                    .queryParam("tableName", tableName)
                    .queryParamIfPresent("catalog", Optional.ofNullable(catalog))
                    .queryParamIfPresent("schema", Optional.ofNullable(schema))
                    .build()
                    .toUri();

            List<TableDefinitionInfo> tableDefinitions = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key",optionalSystem.get().getApiKey())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (tableDefinitions == null || tableDefinitions.isEmpty()) {
                return "没有表结构";
            } else {
                StringBuilder sb = new StringBuilder();
                for (TableDefinitionInfo tableDefinition : tableDefinitions) {
                    sb.append(String.format("catalog: %s schema:%s 表名：%s 表类型：%s 表描述：%s\n", tableDefinition.tableCat(), tableDefinition.tableSchema(), tableDefinition.tableName(), tableDefinition.tableType(), tableDefinition.remarks()));
                    sb.append("列：\n");
                    for (ColumnInfo column : tableDefinition.columns()) {
                        sb.append(String.format("列名：%s 类型：%s 长度：%s 精度：%s 描述：%s 默认值：%s 位置：%s 可空：%s 自增：%s 生成列：%s\n",
                                column.columnName(),
                                column.typeName(),
                                column.columnSize(),
                                column.decimalDigits(),
                                column.remarks(),
                                column.columnDef(),
                                column.ordinalPosition(),
                                column.isNullable(),
                                column.isAutoincrement(),
                                column.isGeneratedcolumn()
                        ));
                    }
                    sb.append("主键：\n");
                    for (PrimaryKeyInfo primaryKey : tableDefinition.primaryKeys()) {
                        sb.append(String.format("主键名：%s 主键值：%s\n", primaryKey.pkName(), primaryKey.columnName()));
                    }
                    sb.append("外键：\n");
                    for (ForeignKeyInfo foreignKey : tableDefinition.foreignKeys()) {
                        sb.append(String.format("外键名：%s 主键名：%s 主键表名：%s 主键列名：%s 外键表名：%s 外键列名：%s\n",
                                foreignKey.fkName(),
                                foreignKey.pkName(),
                                foreignKey.pkTableName(),
                                foreignKey.pkColumnName(),
                                foreignKey.fkTableName(),
                                foreignKey.fkColumnName()
                        ));
                    }
                    sb.append("索引：\n");
                    for (IndexInfo index : tableDefinition.indexes()) {
                        sb.append(String.format("索引名：%s 是否为非唯一索引：%s 索引包含的列名列表：%s 排序方向：%s\n",
                                index.indexName(),
                                index.nonUnique(),
                                index.columnName(),
                                index.ascOrDesc()
                        ));
                    }
                }
                return sb.toString();
            }
        } else {
            return "系统不存在";
        }
    }

    @Tool(description = "根据系统名称，执行SQL查询并返回结果集")
    public String executeSQL(@ToolParam(description = "系统名称") String systemName, @ToolParam(description = "要执行的SQL语句") String sql) {
        Optional<SqlForgeMcpProperties.SystemInfo> optionalSystem = systems.stream()
                .filter(system -> system.getName().equals(systemName))
                .findAny();
        if (optionalSystem.isPresent()) {
            return restClient.post()
                    .uri(optionalSystem.get().getUrl() + Constant.EXECUTE_SQL_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key",optionalSystem.get().getApiKey())
                    .body(Map.of("sql", sql))
                    .retrieve()
                    .body(String.class);
        } else {
            return "系统不存在";
        }
    }

    @Tool(description = "根据系统名称，保存页面JSON配置模板")
    public String amisTemplateSave(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "模板id") String id,
            @ToolParam(description = "模板名称") String name,
            @ToolParam(description = "模板描述") String description,
            @ToolParam(description = "JSON配置模板") String context) {
        Optional<SqlForgeMcpProperties.SystemInfo> optionalSystem = systems.stream()
                .filter(system -> system.getName().equals(systemName))
                .findAny();
        if (optionalSystem.isPresent()) {
            return restClient.put()
                    .uri(optionalSystem.get().getUrl() + Constant.PUT_TEMPLATE_AMIS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key",optionalSystem.get().getApiKey())
                    .body(Map.of("id", id, "name", name, "description", description, "context", context))
                    .retrieve()
                    .body(String.class);
        } else {
            return "系统不存在";
        }
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
    }

}
