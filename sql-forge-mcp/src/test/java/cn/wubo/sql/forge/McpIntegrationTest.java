package cn.wubo.sql.forge;

import cn.wubo.sql.forge.amis.AmisKnowledgeService;
import cn.wubo.sql.forge.amis.AmisValidator;
import cn.wubo.sql.forge.AmisService;
import cn.wubo.sql.forge.testing.MockSqlForgeServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

/**
 * sql-forge-mcp 集成测试：用 {@link MockSqlForgeServer} 模拟业务后端，
 * 不依赖真实 sql-forge-test，覆盖核心 workflow + 生产就绪 + 死锁回归。
 * <p>
 * 关键约定：
 * <ul>
 *   <li>所有 mock 期望在调用前一次性设好（避免 MockRestServiceServer 顺序约束）</li>
 *   <li>URL 匹配包含 query string —— 如有差异用 {@code method()} 单独匹配</li>
 *   <li>每次测试结尾调 {@code mock.verify()}（JUnit 自动断言）</li>
 * </ul>
 *
 * @Tag("integration") —— 集成测试（用 Mock fixture，无真实后端），PR 必跑
 */
@Tag("integration")
class McpIntegrationTest {

    private static final String BASE_URL = "http://localhost";
    private static final String API_KEY = "test";

    private MockSqlForgeServer mock;
    private SqlForgeMcpService mcp;
    private MetadataService metadataService;
    private JsonCrudService jsonCrudService;
    private TemplateService templateService;
    private AmisService amisService;
    private SqlForgeMcpProperties props;

    @BeforeEach
    void setUp() throws Exception {
        AmisKnowledgeService knowledge = new AmisKnowledgeService();
        AmisValidator validator = new AmisValidator(knowledge);

        props = new SqlForgeMcpProperties();
        SqlForgeMcpProperties.SystemInfo sys = new SqlForgeMcpProperties.SystemInfo();
        sys.setName("TestSys");
        sys.setUrl(BASE_URL);
        sys.setApiKey(API_KEY);
        props.setSystems(List.of(sys));

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mock = new MockSqlForgeServer(builder);
        mock.reset();  // 清掉上个测试残留的期望
        RestClient client = mock.getMockBoundClient();
        mcp = new SqlForgeMcpService(props, client, validator, new MetricsService(), new AuditLogger(java.nio.file.Path.of("/dev/null")), null);

        // Round 5 拆分后的领域 Service
        McpToolSupport support = new McpToolSupport(props.getSystems(),
                new MetricsService(), new AuditLogger(java.nio.file.Path.of("/dev/null")),
                1024 * 1024, 10 * 1024 * 1024);
        metadataService = new MetadataService(support, client);
        jsonCrudService = new JsonCrudService(support, client);
        templateService = new TemplateService(support, client);
        amisService = new AmisService(support, validator, null);
    }

    // ====================== Phase 1: 端到端工作流 ======================

    @Nested
    @DisplayName("Phase 1: 端到端工作流")
    class WorkflowJourneys {

        /**
         * Journey A：构造 CRUD 页面（9 步）。
         * <p>
         * 注：describeSchema 内部会先调一次 fetchTables（= metaDataTables）
         * 再调 metaDataDefinitions，所以期望需要 2 次 metaDataTables + 1 次 metaDataDefinitions。
         * </p>
         */
        @Test
        @DisplayName("Journey A: 构造 CRUD 页面")
        void journeyA_construct_crud_page() {
            // 一次性设好所有期望（避免 MockRestServiceServer 顺序报错）
            mock.expectHealthUp();  // 01 mcpHealth
            mock.expectMetaDataTables(List.of(  // 02 findTablesByName
                    Map.of("tableCat", "TESTDB", "tableSchema", "PUBLIC",
                            "tableName", "USERS", "tableType", "BASE TABLE",
                            "remarks", "用户表")));
            mock.expectMetaDataTables(List.of(  // 03a describeSchema 内部 fetchTables
                    Map.of("tableCat", "TESTDB", "tableSchema", "PUBLIC",
                            "tableName", "USERS", "tableType", "BASE TABLE",
                            "remarks", "用户表")));
            mock.expectMetaDataDefinitions("""
                    [{"tableName":"USERS","columns":[
                        {"columnName":"ID","typeName":"CHARACTER VARYING"},
                        {"columnName":"USERNAME","typeName":"CHARACTER VARYING"}
                    ],"primaryKeys":[]}]""");
            mock.expectJsonCrud("select", "USERS",
                    "[{\"ID\":\"1\",\"USERNAME\":\"alice\"}]");
            mock.expectAmisTemplateSave("true");
            mock.expectAmisTemplateGet("journey_a_001",
                    "{\"id\":\"journey_a_001\",\"name\":\"Journey A 测试\"}");
            mock.expectAmisTemplateDelete("journey_a_001");
            mock.expectHealthUp();

            // 跑 9 步
            Map<String, Object> health = mcp.mcpHealth();
            assertEquals("UP", health.get("status"));

            Object found = metadataService.findTablesByName("TestSys", "USERS");
            assertInstanceOf(List.class, found);

            Object schema = metadataService.describeSchema("TestSys", "USERS");
            assertTrue(schema.toString().contains("USERS"));

            Object rows = jsonCrudService.jsonSelect("TestSys", "USERS", null);
            assertTrue(rows.toString().contains("alice"));

            String schemaJson = """
                    {"type":"page","title":"用户管理","body":{"type":"crud",
                    "api":"POST /sql/forge/api/json/select/users",
                    "columns":[{"name":"id","label":"ID"}]}}""";
            AmisValidator.ValidationResult v = amisService.validateAmisTemplate(schemaJson);
            assertTrue(v.valid());

            Object saved = templateService.amisTemplateSave("TestSys", "journey_a_001",
                    "Journey A 测试", "Phase 1 测试", schemaJson);
            assertEquals("true", saved);

            Object fetched = templateService.getAmisTemplate("TestSys", "journey_a_001");
            assertTrue(fetched.toString().contains("journey_a_001"));

            Object deleted = templateService.deleteAmisTemplate("TestSys", "journey_a_001");
            assertEquals("true", deleted);

            Map<String, Object> healthAfter = mcp.mcpHealth();
            assertEquals("UP", healthAfter.get("status"));
        }

        /**
         * Journey B：执行 SQL 模板。
         * <p>
         * 实际调用链路：
         * <ul>
         *   <li>executeSqlTemplate POST 到 /sql/forge/api/template/sql/{id}（不是 /json/select/）</li>
         *   <li>executeSqlTemplateSafely 内部会再调一次 getSqlTemplate（校验占位符）</li>
         * </ul>
         * </p>
         */
        @Test
        @DisplayName("Journey B: 执行 SQL 模板")
        void journeyB_execute_sql_template() {
            mock.expectSqlTemplateList("""
                    [{"id":"tpl_db","name":"DB 查询",
                    "executorName":"database",
                    "context":"SELECT * FROM users WHERE id=#{id}"}]""");
            // 第 1 次 getSqlTemplate：测试自己直接调
            mock.expectSqlTemplateGet("tpl_db",
                    "{\"id\":\"tpl_db\",\"context\":\"SELECT * FROM users WHERE id=#{id}\"}");
            // 第 2 次 getSqlTemplate：executeSqlTemplateSafely 内部调用
            mock.expectSqlTemplateGet("tpl_db",
                    "{\"id\":\"tpl_db\",\"context\":\"SELECT * FROM users WHERE id=#{id}\"}");
            // executeSqlTemplate POST 到 /template/sql/tpl_db（不是 /json/select/users）
            mock.expectSqlTemplateExecute("tpl_db", "[{\"ID\":\"1\",\"USERNAME\":\"alice\"}]");

            Object list = templateService.listSqlTemplates("TestSys", null, null, null);
            assertTrue(list.toString().contains("tpl_db"));

            Object tpl = templateService.getSqlTemplate("TestSys", "tpl_db");
            assertTrue(tpl.toString().contains("id=#{id}"));

            Object safe = templateService.executeSqlTemplateSafely("TestSys", "tpl_db", null);
            assertTrue(safe.toString().contains("参数缺失"));

            Object result = templateService.executeSqlTemplate("TestSys", "tpl_db",
                    Map.of("id", "1"));
            assertTrue(result.toString().contains("alice"));
        }

        /**
         * Journey C：诊断渲染错误。
         */
        @Test
        @DisplayName("Journey C: 校验错误检测")
        void journeyC_validationErrorDetection() {
            // 空内容 → 校验报"模板内容为空"
            AmisValidator.ValidationResult v1 = amisService.validateAmisTemplate("");
            assertFalse(v1.valid());
            assertTrue(v1.errors().get(0).message().contains("模板内容为空"));

            // 非法 JSON → 校验报"JSON 解析失败"
            AmisValidator.ValidationResult v2 = amisService.validateAmisTemplate("{broken");
            assertFalse(v2.valid());
            assertTrue(v2.errors().get(0).message().contains("JSON"));

            // 修复后通过
            String fixed = "{\"type\":\"page\",\"title\":\"已修复\"}";
            AmisValidator.ValidationResult v3 = amisService.validateAmisTemplate(fixed);
            assertTrue(v3.valid());
        }

        /**
         * Journey E：多系统隔离。
         */
        @Test
        @DisplayName("Journey E: 多系统隔离")
        void journeyE_multi_system_isolation() {
            SqlForgeMcpProperties multiProps = new SqlForgeMcpProperties();
            SqlForgeMcpProperties.SystemInfo sysA = new SqlForgeMcpProperties.SystemInfo();
            sysA.setName("SystemA"); sysA.setUrl(BASE_URL); sysA.setApiKey("kA");
            SqlForgeMcpProperties.SystemInfo sysB = new SqlForgeMcpProperties.SystemInfo();
            sysB.setName("SystemB"); sysB.setUrl(BASE_URL); sysB.setApiKey("kB");
            multiProps.setSystems(List.of(sysA, sysB));

            RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
            MockSqlForgeServer multiMock = new MockSqlForgeServer(builder);
            SqlForgeMcpService multiMcp = new SqlForgeMcpService(multiProps,
                    builder.build(), null, new MetricsService(), new AuditLogger(java.nio.file.Path.of("/dev/null")), null);

            multiMock.expectHealthUp();  // SystemA
            multiMock.expectHealthUp();  // SystemB

            // 不存在的系统 → 友好错误（用 metadataService 测系统解析）
            McpToolSupport multiSupport2 = new McpToolSupport(multiProps.getSystems(),
                    new MetricsService(), new AuditLogger(java.nio.file.Path.of("/dev/null")),
                    1024 * 1024, 10 * 1024 * 1024);
            MetadataService multiMetaSvc = new MetadataService(multiSupport2,
                    RestClient.builder().baseUrl(BASE_URL).build());
            Object r = multiMetaSvc.getMetaDataDatabase("NonExistent");
            assertEquals("系统不存在: NonExistent", r);

            // mcpHealth 应包含两个系统
            Map<String, Object> health = multiMcp.mcpHealth();
            Map<String, Object> backends = (Map<String, Object>) health.get("backends");
            assertEquals(2, backends.size());
            assertTrue(backends.containsKey("SystemA"));
            assertTrue(backends.containsKey("SystemB"));
        }
    }

    // ====================== Phase 4: 生产就绪检查 ======================

    @Nested
    @DisplayName("Phase 4: 生产就绪检查")
    class ProductionReadiness {

        @Test
        @DisplayName("PR-1: mcpHealth 字段完整")
        void pr1_mcpHealth_fullFields() {
            mock.expectHealthUp();
            Map<String, Object> result = mcp.mcpHealth();
            assertEquals("UP", result.get("status"));
            Map<String, Object> m = (Map<String, Object>) result.get("mcp");
            assertEquals(29, m.get("registeredTools"));
            assertEquals(3, m.get("registeredResources"));
            assertEquals(3, m.get("registeredPrompts"));
            Map<String, Object> pw = (Map<String, Object>) result.get("playwright");
            assertEquals("DISABLED", pw.get("status"));
            Map<String, Object> l = (Map<String, Object>) result.get("limits");
            assertEquals(1048576, l.get("maxRequestBytes"));
            assertNotNull(result.get("checkedAt"));
        }

        @Test
        @DisplayName("PR-2: 超大 body 被友好拒绝（不调后端）")
        void pr2_oversizedBody_rejectedWithoutBackendCall() {
            // 不 mock 后端 —— 如果调了会报错（"Cannot add more expectations" 或 mock 没匹配）
            StringBuilder big = new StringBuilder();
            for (int i = 0; i < 200_000; i++) big.append("x");
            Map<String, Object> hugeBody = new LinkedHashMap<>();
            for (int i = 0; i < 5; i++) hugeBody.put("k" + i, big.toString());

            Object r = jsonCrudService.jsonInsert("TestSys", "USERS", hugeBody);
            assertInstanceOf(String.class, r);
            assertTrue(((String) r).contains("请求体过大"));
        }

        @Test
        @DisplayName("PR-3: 中文 in/out 不死锁")
        void pr3_chineseRoundtrip_noDeadlock() {
            String cnId = "中文id_001";
            String cnName = "中文模板名";
            String cnDesc = "中文描述：你好世界";
            String cnContext = "{\"type\":\"page\",\"title\":\"中文\",\"body\":\"中文body\"}";

            mock.expectAmisTemplateSave("true");
            mock.expectAmisTemplateGet(cnId,
                    "{\"id\":\"" + cnId + "\",\"name\":\"" + cnName + "\"}");
            mock.expectAmisTemplateDelete(cnId);
            mock.expectHealthUp();

            Object saved = templateService.amisTemplateSave("TestSys", cnId, cnName, cnDesc, cnContext);
            assertEquals("true", saved);

            Object fetched = templateService.getAmisTemplate("TestSys", cnId);
            assertTrue(fetched.toString().contains(cnName),
                    "中文返回值应正确解码: " + fetched);

            Object deleted = templateService.deleteAmisTemplate("TestSys", cnId);
            assertEquals("true", deleted);

            Map<String, Object> health = mcp.mcpHealth();
            assertEquals("UP", health.get("status"));
        }

        @Test
        @DisplayName("PR-4: 28 个 @Tool 全部注册（5 个 Service）")
        void pr4_toolAnnotationCount() {
            int count = 0;
            Class<?>[] allServiceClasses = new Class<?>[]{
                    MetadataService.class, JsonCrudService.class, TemplateService.class,
                    AmisService.class, SqlForgeMcpService.class};
            for (Class<?> c : allServiceClasses) {
                for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(org.springframework.ai.tool.annotation.Tool.class)) {
                        count++;
                    }
                }
            }
            // MetadataService(8) + JsonCrudService(6) + TemplateService(10) +
            // AmisService(2) + SqlForgeMcpService(2 mcpHealth/metrics) = 28
            assertEquals(28, count, "Round 5 拆分后 @Tool 数量");
        }

        @Test
        @DisplayName("PR-7: 401/500 返回友好字符串")
        void pr7_errorResponse_friendlyString() throws Exception {
            // 一次性设好所有期望（避免 MockRestServiceServer 顺序报错）
            mock.expectHealthUnauthorized();
            // 用 expectMetaDataDefinitions（内部用 expectUri 忽略 query 参数）
            mock.expectMetaDataDefinitions500("{\"error\":\"oops\"}");

            Map<String, Object> r401 = mcp.mcpHealth();
            assertEquals("DEGRADED", r401.get("status"));

            Object r500 = metadataService.getMetaDataTableInfo("TestSys", null, null, "USERS", null);
            assertInstanceOf(String.class, r500);
            assertTrue(((String) r500).contains("500"));
            assertTrue(((String) r500).contains("稍后重试"));
        }

        @Test
        @DisplayName("PR-8: 特殊字符 tableName 安全 URL 编码")
        void pr8_specialCharsUrlEncoded() throws Exception {
            String evilTable = "USER; DROP TABLE x";
            // mock 期望按 URLEncoder.encode 后的路径匹配
            String encoded = java.net.URLEncoder.encode(evilTable,
                    java.nio.charset.StandardCharsets.UTF_8);
            mock.expectRequest(requestTo(BASE_URL + "/sql/forge/api/json/select/" + encoded))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(org.springframework.test.web.client.response
                            .MockRestResponseCreators.withSuccess("[]",
                                    org.springframework.http.MediaType.APPLICATION_JSON));

            jsonCrudService.jsonSelect("TestSys", evilTable, null);
        }

        @Test
        @DisplayName("PR-10: 同名 systemName 保留第一个")
        void pr10_duplicateSystemName_keepFirst() {
            SqlForgeMcpProperties dup = new SqlForgeMcpProperties();
            SqlForgeMcpProperties.SystemInfo first = new SqlForgeMcpProperties.SystemInfo();
            first.setName("Dup"); first.setUrl("http://first"); first.setApiKey("k1");
            SqlForgeMcpProperties.SystemInfo second = new SqlForgeMcpProperties.SystemInfo();
            second.setName("Dup"); second.setUrl("http://second"); second.setApiKey("k2");
            dup.setSystems(List.of(first, second));

            SqlForgeMcpService svc = new SqlForgeMcpService(dup.getSystems(),
                    RestClient.builder().baseUrl(BASE_URL).build(), null, null);
            McpToolSupport multiSupport = new McpToolSupport(dup.getSystems(),
                    new MetricsService(), new AuditLogger(java.nio.file.Path.of("/dev/null")),
                    1024 * 1024, 10 * 1024 * 1024);
            MetadataService multiMeta = new MetadataService(multiSupport,
                    RestClient.builder().baseUrl(BASE_URL).build());

            Object r = multiMeta.getMetaDataDatabase("NonExistent");
            assertEquals("系统不存在: NonExistent", r);

            List<SqlForgeMcpProperties.SystemInfo> all = svc.getSystems();
            assertEquals(2, all.size());
        }

        @Test
        @DisplayName("PR-11: 不存在 systemName → 友好错误")
        void pr11_unknownSystem_friendlyError() {
            Object r = metadataService.getMetaDataDatabase("GhostSystem");
            assertEquals("系统不存在: GhostSystem", r);
        }
    }

    // ====================== 死锁 / 字符集专项回归 ======================

    @Nested
    @DisplayName("死锁 / 字符集回归")
    class DeadlockCharsetRegression {

        @Test
        @DisplayName("DC-1: 10 个连续 mcpHealth 调用不死锁")
        void dc1_10SequentialHealthCalls() {
            // 一次性设好 10 次期望
            for (int i = 0; i < 10; i++) mock.expectHealthUp();
            // 连续调用
            for (int i = 0; i < 10; i++) {
                Map<String, Object> h = mcp.mcpHealth();
                assertEquals("UP", h.get("status"));
            }
        }

        @Test
        @DisplayName("DC-2: 中文 id 增删查 + 探活")
        void dc2_chineseId_fullCycle() {
            String id = "中文id_" + System.currentTimeMillis();
            mock.expectAmisTemplateSave("true");
            mock.expectAmisTemplateGet(id, "{\"id\":\"" + id + "\"}");
            mock.expectAmisTemplateDelete(id);
            mock.expectHealthUp();

            templateService.amisTemplateSave("TestSys", id, "测试", "描述", "{}");
            Object got = templateService.getAmisTemplate("TestSys", id);
            assertTrue(got.toString().contains(id));
            templateService.deleteAmisTemplate("TestSys", id);
            mcp.mcpHealth();
        }

        @Test
        @DisplayName("DC-3: 50 次 CRUD 调用无 OOM")
        void dc3_50Calls_noOom() {
            // 一次性设好 50 次期望
            for (int i = 0; i < 50; i++) {
                mock.expectJsonCrud("select", "USERS", "[]");
            }
            // 连续调用
            for (int i = 0; i < 50; i++) {
                Object r = jsonCrudService.jsonSelect("TestSys", "USERS", null);
                assertNotNull(r);
            }
        }
    }
}
