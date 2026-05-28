package cn.wubo.sql.forge;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SqlForgeMcpService {

    private final List<SqlForgeMcpProperties.SystemInfo> systems;
    private final RestClient restClient;

    private final static String GET_METADATA_DATABASE_URL = "/sql/forge/api/database/metaDataDatabase";
    private final static String GET_TABLE_TYPES_URL = "/sql/forge/api/database/tableTypes";
    private final static String GET_METADATA_TABLES_URL = "/sql/forge/api/database/metaDataTables";
    private final static String GET_METADATA_TABLE_INFO_URL = "/sql/forge/api/database/metaDataTableInfo";
    private final static String EXECUTE_SQL_URL = "/sql/forge/api/database/execute";
    private final static String SAVE_TEMPLATE_AMIS_URL = "/sql/forge/api/template/amis/save";


    public SqlForgeMcpService(List<SqlForgeMcpProperties.SystemInfo> systems, RestClient restClient) {
        this.systems = systems;
        this.restClient = restClient;
    }

    @Tool(description = "获取所有系统信息")
    private String getSystems() {
        StringBuilder sb = new StringBuilder();
        sb.append("系统列表：\n");
        sb.append(String.format("%-10s %-50s%n", "系统名称", "系统描述"));
        for (SqlForgeMcpProperties.SystemInfo system : systems) {
            sb.append(String.format("%-10s %-50s%n", system.getName(), system.getDescription()));
        }
        return sb.toString();
    }

    @Tool(description = "获取系统数据库信息，包括 productName-数据库产品名称, productVersion-数据库产品版本")
    private String getMetaDataDatabase(@ToolParam(description = "系统名称") String systemName) {
        Optional<SqlForgeMcpProperties.SystemInfo> optionalSystem = systems.stream()
                .filter(system -> system.getName().equals(systemName))
                .findAny();
        if (optionalSystem.isPresent()) {
            return restClient.get()
                    .uri(optionalSystem.get().getUrl() + GET_METADATA_DATABASE_URL)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } else {
            return "系统不存在";
        }
    }

    @Tool(description = "获取系统数据库表类型")
    private String sqlForgeMetaDataTableTypes(@ToolParam(description = "系统名称") String systemName) {
        Optional<SqlForgeMcpProperties.SystemInfo> optionalSystem = systems.stream()
                .filter(system -> system.getName().equals(systemName))
                .findAny();
        if (optionalSystem.isPresent()) {
            return restClient.get()
                    .uri(optionalSystem.get().getUrl() + GET_TABLE_TYPES_URL)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } else {
            return "系统不存在";
        }
    }

    @Tool(description = "获取系统数据库所有表的信息，包括 tableName-表名，tableSchema-schema, tableType-表类型, remarks-表描述")
    private String sqlForgeMetaDataTables(@ToolParam(description = "系统名称") String systemName) {
        Optional<SqlForgeMcpProperties.SystemInfo> optionalSystem = systems.stream()
                .filter(system -> system.getName().equals(systemName))
                .findAny();
        if (optionalSystem.isPresent()) {
            return restClient.get()
                    .uri(optionalSystem.get().getUrl() + GET_METADATA_TABLES_URL)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } else {
            return "系统不存在";
        }
    }

    @Tool(description = "获取系统数据库表结构, 包括 tableName-表名，tableSchema-schema, tableType-表类型, remarks-表描述, columns-列, primaryKeys-主键, foreignKeys-外键, indexes-索引")
    private String getMetaDataTableInfo(
            @ToolParam(description = "系统名称") String systemName,
            @ToolParam(description = "catalog", required = false) String catalog,
            @ToolParam(description = "schema", required = false) String schema,
            @ToolParam(description = "tableName") String tableName,
            @ToolParam(description = "tableType") String tableType
    ) {
        Optional<SqlForgeMcpProperties.SystemInfo> optionalSystem = systems.stream()
                .filter(system -> system.getName().equals(systemName))
                .findAny();
        if (optionalSystem.isPresent()) {
            return restClient.get()
                    .uri(optionalSystem.get().getUrl() + GET_METADATA_TABLE_INFO_URL)
                    .attribute("catalog", catalog)
                    .attribute("schema", schema)
                    .attribute("tableType", tableType)
                    .attribute("tableName", tableName)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } else {
            return "系统不存在";
        }
    }

    @Tool(description = "执行SQL查询并返回结果集")
    public String executeSQL(@ToolParam(description = "系统名称") String systemName, @ToolParam(description = "要执行的SQL语句") String sql) {
        Optional<SqlForgeMcpProperties.SystemInfo> optionalSystem = systems.stream()
                .filter(system -> system.getName().equals(systemName))
                .findAny();
        if (optionalSystem.isPresent()) {
            return restClient.post()
                    .uri(optionalSystem.get().getUrl() + EXECUTE_SQL_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("sql", sql))
                    .retrieve()
                    .body(String.class);
        } else {
            return "系统不存在";
        }
    }

    @Tool(description = "保存页面JSON配置模板")
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
                    .uri(optionalSystem.get().getUrl() + SAVE_TEMPLATE_AMIS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("id", id, "name", name, "description", description, "context", context))
                    .retrieve()
                    .body(String.class);
        } else {
            return "系统不存在";
        }
    }


    public static void main(String[] args) {
        RestClient restClient = RestClient.builder().baseUrl("http://localhost:8081")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();

        SqlForgeMcpProperties.SystemInfo systemInfo = new SqlForgeMcpProperties.SystemInfo();
        systemInfo.setName("系统名称");
        systemInfo.setUrl("http://localhost:8081");
        systemInfo.setDescription("系统描述");


        SqlForgeMcpService sqlForgeMcpService = new SqlForgeMcpService(List.of(systemInfo), restClient);

        String database = sqlForgeMcpService.getMetaDataDatabase(systemInfo.getName());
        System.out.println(database);

        String tables = sqlForgeMcpService.sqlForgeMetaDataTables(systemInfo.getName());
        System.out.println(tables);

        String tableInfo = sqlForgeMcpService.getMetaDataTableInfo(systemInfo.getName(), null, null, "users", "TABLE");
        System.out.println(tableInfo);
    }

}
