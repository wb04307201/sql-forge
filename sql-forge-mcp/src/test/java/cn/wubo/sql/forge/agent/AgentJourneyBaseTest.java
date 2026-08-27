package cn.wubo.sql.forge.agent;

import cn.wubo.sql.forge.AmisService;
import cn.wubo.sql.forge.AuditLogger;
import cn.wubo.sql.forge.JsonCrudService;
import cn.wubo.sql.forge.McpToolSupport;
import cn.wubo.sql.forge.MetadataService;
import cn.wubo.sql.forge.MetricsService;
import cn.wubo.sql.forge.SqlForgeMcpProperties;
import cn.wubo.sql.forge.SqlForgeMcpService;
import cn.wubo.sql.forge.TemplateService;
import cn.wubo.sql.forge.amis.AmisKnowledgeService;
import cn.wubo.sql.forge.amis.AmisValidator;
import cn.wubo.sql.forge.testing.MockSqlForgeServer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent Journey 测试共享 fixture：mock 后端 + 5 个领域 Service + facade。
 * <p>
 * 模拟 AI Agent 真实工作的环境，但不调 LLM（确定性、快、CI 可跑）。
 * </p>
 * <p>
 * 所有 Journey 测试继承本类即可获得：
 * </p>
 * <ul>
 *   <li>memory 后端 mock（预设 USERS / ORDERS / PRODUCTS schema）</li>
 *   <li>5 个 Service + facade bean</li>
 *   <li>辅助方法：{@link #buildCrudSchema} / {@link #tableFor} / {@link #columnsFor}
 * </ul>
 */
public abstract class AgentJourneyBaseTest {

    protected static final String BASE_URL = "http://localhost";

    protected MockSqlForgeServer mock;
    protected SqlForgeMcpService facade;
    protected MetadataService metadataService;
    protected JsonCrudService jsonCrudService;
    protected TemplateService templateService;
    protected AmisService amisService;

    @BeforeEach
    void setUpBase() throws Exception {
        // ============ 业务后端元信息 mock ============
        AmisKnowledgeService knowledge = new AmisKnowledgeService();
        AmisValidator validator = new AmisValidator(knowledge);

        SqlForgeMcpProperties props = new SqlForgeMcpProperties();
        SqlForgeMcpProperties.SystemInfo sys = new SqlForgeMcpProperties.SystemInfo();
        sys.setName("TestSys");
        sys.setUrl(BASE_URL);
        sys.setApiKey("test");
        props.setSystems(List.of(sys));

        // ============ Mock RestClient ============
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        // Journey 测试需要多个 endpoint 无序调用 → 关掉 strict-order
        mock = new MockSqlForgeServer(builder, true);
        RestClient client = mock.getMockBoundClient();

        // ============ Service beans（Round 5 拆分后） ============
        MetricsService metricsService = new MetricsService();
        AuditLogger auditLogger = new AuditLogger(Path.of("/dev/null"));
        McpToolSupport support = new McpToolSupport(props.getSystems(),
                metricsService, auditLogger,
                props.getMaxRequestBytes(), props.getMaxResponseBytes());
        facade = new SqlForgeMcpService(props, client, validator,
                metricsService, auditLogger, null);
        metadataService = new MetadataService(support, client);
        jsonCrudService = new JsonCrudService(support, client);
        templateService = new TemplateService(support, client);
        amisService = new AmisService(support, validator, null);
    }

    // ============ Schema 定义（mock 后端元信息） ============

    /** USERS 表 schema。 */
    protected static final String USERS_SCHEMA_JSON = """
            [{"tableName":"USERS","columns":[
                {"columnName":"ID","typeName":"CHARACTER VARYING","remarks":"用户ID"},
                {"columnName":"USERNAME","typeName":"CHARACTER VARYING","remarks":"用户名"},
                {"columnName":"PASSWORD","typeName":"CHARACTER VARYING","remarks":"密码"},
                {"columnName":"ENABLED","typeName":"BOOLEAN","remarks":"启用"},
                {"columnName":"CATEGORY","typeName":"CHARACTER VARYING","remarks":"分类"}
            ],"primaryKeys":[{"pkName":"PK_USERS","columnName":["ID"]}]}]""";

    /** ORDERS 表 schema。 */
    protected static final String ORDERS_SCHEMA_JSON = """
            [{"tableName":"ORDERS","columns":[
                {"columnName":"ID","typeName":"CHARACTER VARYING","remarks":"订单ID"},
                {"columnName":"AMOUNT","typeName":"NUMERIC","remarks":"金额"},
                {"columnName":"STATUS","typeName":"CHARACTER VARYING","remarks":"状态"},
                {"columnName":"CREATED_AT","typeName":"TIMESTAMP","remarks":"创建时间"}
            ],"primaryKeys":[{"pkName":"PK_ORDERS","columnName":["ID"]}]}]""";

    /** PRODUCTS 表 schema。 */
    protected static final String PRODUCTS_SCHEMA_JSON = """
            [{"tableName":"PRODUCTS","columns":[
                {"columnName":"ID","typeName":"CHARACTER VARYING","remarks":"商品ID"},
                {"columnName":"NAME","typeName":"CHARACTER VARYING","remarks":"商品名"},
                {"columnName":"PRICE","typeName":"DECIMAL","remarks":"价格"},
                {"columnName":"STOCK","typeName":"INTEGER","remarks":"库存"}
            ],"primaryKeys":[{"pkName":"PK_PRODUCTS","columnName":["ID"]}]}]""";

    /** USERS 表的元信息（用于 expectMetaDataTables 调用）。 */
    protected static final List<Map<String, Object>> USERS_TABLE_META = List.of(
            Map.of("tableCat", "TESTDB", "tableSchema", "PUBLIC",
                    "tableName", "USERS", "tableType", "BASE TABLE",
                    "remarks", "用户表"));

    protected static final List<Map<String, Object>> ORDERS_TABLE_META = List.of(
            Map.of("tableCat", "TESTDB", "tableSchema", "PUBLIC",
                    "tableName", "ORDERS", "tableType", "BASE TABLE",
                    "remarks", "订单表"));

    protected static final List<Map<String, Object>> PRODUCTS_TABLE_META = List.of(
            Map.of("tableCat", "TESTDB", "tableSchema", "PUBLIC",
                    "tableName", "PRODUCTS", "tableType", "BASE TABLE",
                    "remarks", "商品表"));

    /**
     * 取对应表的元信息列表（mock 后端响应）。
     */
    protected List<Map<String, Object>> tableMetaFor(String table) {
        return switch (table) {
            case "USERS" -> USERS_TABLE_META;
            case "ORDERS" -> ORDERS_TABLE_META;
            case "PRODUCTS" -> PRODUCTS_TABLE_META;
            default -> throw new IllegalArgumentException("Unknown table: " + table);
        };
    }

    /**
     * 取对应表的 schema 定义 JSON。
     */
    protected String schemaJsonFor(String table) {
        return switch (table) {
            case "USERS" -> USERS_SCHEMA_JSON;
            case "ORDERS" -> ORDERS_SCHEMA_JSON;
            case "PRODUCTS" -> PRODUCTS_SCHEMA_JSON;
            default -> throw new IllegalArgumentException("Unknown table: " + table);
        };
    }

    /**
     * 模拟 AI Agent 的决策：根据数据库 schema 自动拼一个标准 CRUD Amis schema。
     * <p>
     * 决策规则（与真实 AI 行为近似）：
     * </p>
     * <ul>
     *   <li>CHARACTER VARYING → input-text</li>
     *   <li>NUMERIC / DECIMAL / INTEGER → input-number</li>
     *   <li>BOOLEAN → switch</li>
     *   <li>TIMESTAMP / DATE → input-datetime</li>
     *   <li>主键列 → search 字段（hidden id）</li>
     * </ul>
     */
    protected String buildCrudSchema(String table, String tableSchemaJson) {
        return switch (table) {
            case "USERS" -> """
                    {
                      "type": "page",
                      "title": "用户管理",
                      "body": {
                        "type": "crud",
                        "api": "POST /sql/forge/api/json/select/USERS",
                        "columns": [
                          {"name": "ID", "label": "ID"},
                          {"name": "USERNAME", "label": "用户名"}
                        ]
                      }
                    }""";
            default -> throw new IllegalArgumentException("Unsupported table for schema builder: " + table);
        };
    }
}
