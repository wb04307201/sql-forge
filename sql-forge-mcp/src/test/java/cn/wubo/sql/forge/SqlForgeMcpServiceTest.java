package cn.wubo.sql.forge;

import cn.wubo.sql.forge.amis.AmisValidator;
import cn.wubo.sql.forge.amis.PlaywrightRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * SqlForgeMcpService 单元测试：覆盖 systemName 解析、URL 路径编码、错误信息友好化。
 * <p>
 * 使用 {@link MockRestServiceServer} 拦截 RestClient 请求，避免起真实后端。
 * </p>
 */
@Tag("fast")
class SqlForgeMcpServiceTest {

    private MockRestServiceServer mockServer;
    private SqlForgeMcpService mcp;
    private MetadataService metadataService;
    private JsonCrudService jsonCrudService;
    private TemplateService templateService;
    private McpToolSupport support;
    private SqlForgeMcpProperties.SystemInfo sys;

    @BeforeEach
    void setUp() throws Exception {
        sys = new SqlForgeMcpProperties.SystemInfo();
        sys.setName("TestSys");
        sys.setUrl("http://localhost:8081");
        sys.setApiKey("test-key");

        // 用 baseUrl() 绑定的 RestClient.Builder 建客户端，让 MockRestServiceServer 能拦截
        Builder builder = RestClient.builder().baseUrl("http://localhost:8081");
        mockServer = MockRestServiceServer.bindTo(builder).build();

        RestClient client = builder.build();
        mcp = new SqlForgeMcpService(List.of(sys), client, null, null);
        // Round 5 拆分后的领域 Service
        support = new McpToolSupport(List.of(sys),
                new MetricsService(), new AuditLogger(java.nio.file.Path.of("/dev/null")),
                1024 * 1024, 10 * 1024 * 1024);
        metadataService = new MetadataService(support, client);
        jsonCrudService = new JsonCrudService(support, client);
        templateService = new TemplateService(support, client);
    }

    // ============ getSystems ============

    @Test
    void getSystems_returnsAllRegisteredSystems() {
        List<SqlForgeMcpProperties.SystemInfo> result = mcp.getSystems();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TestSys", result.get(0).getName());
        assertEquals("http://localhost:8081", result.get(0).getUrl());
        assertEquals("test-key", result.get(0).getApiKey());
    }

    // ============ resolveSystem: 系统名找不到 ============

    @Test
    void unknownSystem_returnsFriendlyError() {
        Object result = metadataService.getMetaDataDatabase("NonExistent");
        assertInstanceOf(String.class, result);
        assertEquals("系统不存在: NonExistent", result);
    }

    // ============ URL path 段编码 ============

    @Test
    void jsonSelect_encodesTableName() {
        // 含特殊字符的表名应被 URL 编码
        mockServer.expect(requestTo("http://localhost:8081/sql/forge/api/json/select/USER%3B_DROP"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        Object result = jsonCrudService.jsonSelect("TestSys", "USER;_DROP", null);
        mockServer.verify();
        assertNotNull(result);
    }

    @Test
    void getSqlTemplate_encodesId() {
        // URLEncoder.encode("my template", UTF-8) -> "my+template"（application/x-www-form-urlencoded 风格）
        mockServer.expect(requestTo("http://localhost:8081/sql/forge/api/template/sql/my+template"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"my template\"}", MediaType.APPLICATION_JSON));

        Object result = templateService.getSqlTemplate("TestSys", "my template");
        mockServer.verify();
        assertNotNull(result);
    }

    // ============ 错误信息友好化 ============

    @Test
    void http401_returnsAuthFailureHint() {
        mockServer.expect(requestTo("http://localhost:8081/sql/forge/api/database/metaDataDatabase"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        Object result = metadataService.getMetaDataDatabase("TestSys");
        assertInstanceOf(String.class, result);
        String msg = (String) result;
        assertTrue(msg.contains("认证失败"), "401 应提示认证失败: " + msg);
        assertTrue(msg.contains("test-key") || msg.contains("apiKey"), "401 应提到 apiKey: " + msg);
    }

    @Test
    void http404_returnsPathNotFoundHint() {
        mockServer.expect(requestTo("http://localhost:8081/sql/forge/api/database/metaDataDatabase"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        Object result = metadataService.getMetaDataDatabase("TestSys");
        assertInstanceOf(String.class, result);
        String msg = (String) result;
        assertTrue(msg.contains("404") || msg.contains("路径不存在"), "404 应提示路径不存在: " + msg);
    }

    @Test
    void http500_returnsServerErrorHint() {
        mockServer.expect(requestTo("http://localhost:8081/sql/forge/api/database/metaDataDatabase"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("something broke"));

        Object result = metadataService.getMetaDataDatabase("TestSys");
        assertInstanceOf(String.class, result);
        String msg = (String) result;
        assertTrue(msg.contains("500"), "500 应提到状态码: " + msg);
        assertTrue(msg.contains("稍后重试") || msg.contains("管理员"), "500 应提示重试或联系管理员: " + msg);
    }

    @Test
    void connectionRefused_returnsNetworkHint() {
        mockServer.expect(requestTo("http://localhost:8081/sql/forge/api/database/metaDataDatabase"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withException(new java.net.ConnectException("Connection refused")));

        Object result = metadataService.getMetaDataDatabase("TestSys");
        assertInstanceOf(String.class, result);
        String msg = (String) result;
        assertTrue(msg.contains("无法连接") || msg.contains("Connection refused"),
                "网络异常应提示连接问题: " + msg);
    }

    // ============ URL 安全：保留合法标识符 ============

    @Test
    void jsonSelect_keepsLegalIdentifierUnchanged() {
        // 普通标识符不应被错误地双重编码
        mockServer.expect(requestTo("http://localhost:8081/sql/forge/api/json/select/users"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        jsonCrudService.jsonSelect("TestSys", "users", null);
        mockServer.verify();
    }

    // ============ P0-1: mcp_health ============

    @Test
    @SuppressWarnings("unchecked")
    void mcpHealth_backendUp_reportsOverallUP() {
        mockServer.expect(requestTo("http://localhost:8081/sql/forge/api/database/metaDataDatabase"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("{\"productName\":\"H2\"}", MediaType.APPLICATION_JSON));

        java.util.Map<String, Object> result = mcp.mcpHealth();

        assertEquals("UP", result.get("status"));
        java.util.Map<String, Object> mcpInfo = (java.util.Map<String, Object>) result.get("mcp");
        assertEquals("UP", mcpInfo.get("status"));
        assertEquals(1, mcpInfo.get("systemCount"));
        assertEquals(29, mcpInfo.get("registeredTools"));  // 27 + mcp_health + metrics

        java.util.Map<String, Object> backends = (java.util.Map<String, Object>) result.get("backends");
        java.util.Map<String, Object> testSysHealth = (java.util.Map<String, Object>) backends.get("TestSys");
        assertEquals("UP", testSysHealth.get("status"));
        assertTrue(testSysHealth.containsKey("latencyMs"));

        java.util.Map<String, Object> pw = (java.util.Map<String, Object>) result.get("playwright");
        assertEquals("DISABLED", pw.get("status"));  // 测试里没传 renderer

        java.util.Map<String, Object> limits = (java.util.Map<String, Object>) result.get("limits");
        assertEquals(1024 * 1024, limits.get("maxRequestBytes"));
        assertEquals(10 * 1024 * 1024, limits.get("maxResponseBytes"));

        assertTrue(result.containsKey("checkedAt"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void mcpHealth_backendDown_reportsOverallDEGRADED() {
        mockServer.expect(requestTo("http://localhost:8081/sql/forge/api/database/metaDataDatabase"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withException(new java.net.ConnectException("Connection refused")));

        java.util.Map<String, Object> result = mcp.mcpHealth();
        assertEquals("DEGRADED", result.get("status"));

        java.util.Map<String, Object> backends = (java.util.Map<String, Object>) result.get("backends");
        java.util.Map<String, Object> testSysHealth = (java.util.Map<String, Object>) backends.get("TestSys");
        assertEquals("DOWN", testSysHealth.get("status"));
        // 不强制要求具体异常类名（mock 和真实环境不同），只要求有错误信息字段
        assertNotNull(testSysHealth.get("error"));
        assertFalse(testSysHealth.get("error").toString().isBlank());
    }

    // ============ P0-3: 请求体大小限制 ============

    @Test
    void jsonInsert_oversizedBody_rejectsWithFriendlyMessage() {
        // 构造一个超大 body（> 默认 1 MiB 上限）
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 200_000; i++) big.append("x");  // 200K chars × 2 byte/char 估算 = 400K
        // 重复多个字段堆到超过 1 MiB
        java.util.Map<String, Object> bigBody = new java.util.HashMap<>();
        for (int i = 0; i < 5; i++) bigBody.put("k" + i, big.toString());

        // 不需要 mock 后端 —— 大小校验在 HTTP 调用之前就抛
        Object result = jsonCrudService.jsonInsert("TestSys", "USERS", bigBody);
        assertInstanceOf(String.class, result);
        String msg = (String) result;
        assertTrue(msg.contains("请求体过大"), "应提示请求体过大: " + msg);
        assertTrue(msg.contains("减小"), "应给修复建议: " + msg);
    }

    @Test
    void jsonUpdate_smallBody_passesSizeCheck() {
        // 小 body 应通过大小校验，到达后端（这里用 mockServer 拦截）
        mockServer.expect(requestTo("http://localhost:8081/sql/forge/api/json/update/USERS"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"affected\":1}", MediaType.APPLICATION_JSON));

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("@set", java.util.Map.of("name", "alice"));
        body.put("@where", java.util.List.of(java.util.Map.of("column", "id", "condition", "EQ", "value", "1")));

        Object result = jsonCrudService.jsonUpdate("TestSys", "USERS", body);
        mockServer.verify();
        assertNotNull(result);
    }

    @Test
    void jsonDelete_emptyBody_passesSizeCheck() {
        // 空 body（jsonDelete 不带 @where 时）应通过校验，不抛
        mockServer.expect(requestTo("http://localhost:8081/sql/forge/api/json/delete/USERS"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"affected\":0}", MediaType.APPLICATION_JSON));

        Object result = jsonCrudService.jsonDelete("TestSys", "USERS", null);
        mockServer.verify();
        assertNotNull(result);
    }
}
