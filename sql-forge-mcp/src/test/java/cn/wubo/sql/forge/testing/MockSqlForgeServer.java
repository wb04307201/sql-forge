package cn.wubo.sql.forge.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 业务后端 Mock Server —— 拦截 RestClient 发出的 HTTP 请求，返回预设响应。
 * <p>
 * 用于 {@link cn.wubo.sql.forge.SqlForgeMcpService} 的集成测试，
 * 避免依赖真实的 sql-forge-test 后端，让 CI 跑得更快、更稳定。
 * </p>
 * <p>
 * 用法：
 * </p>
 * <pre>{@code
 * RestClient.Builder builder = RestClient.builder().baseUrl("http://mock");
 * MockSqlForgeServer mock = new MockSqlForgeServer(builder);
 *
 * mock.expectHealthUp();
 * mock.expectJsonCrud("select", "USERS", List.of(Map.of("ID", "1", "USERNAME", "alice")));
 * mock.expectAmisTemplateGet("test_id", "{\"type\":\"page\"}");
 *
 * SqlForgeMcpService mcp = new SqlForgeMcpService(props, builder.build(), null, null);
 * mcp.jsonSelect("TestSys", "USERS", null);
 * mock.verify();
 * }</pre>
 */
public class MockSqlForgeServer {

    private final MockRestServiceServer server;
    /**
     * 保存传入的 RestClient.Builder 引用（{@code bindTo} 会就地修改它使后续 {@code build()} 返回 mock-aware client）。
     */
    private final RestClient.Builder mockBoundBuilder;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;

    /**
     * 绑定到一个 RestClient.Builder，后续用 {@code builder.build()} 创建的 client
     * 都会被本 mock 拦截。
     *
     * @param builder RestClient.Builder（必须和实际使用的 client 用同一个）
     */
    public MockSqlForgeServer(RestClient.Builder builder) {
        this(builder, false);
    }

    /**
     * @param ignoreOrder true → 调用顺序不敏感（性能 / 高并发测试用）
     */
    public MockSqlForgeServer(RestClient.Builder builder, boolean ignoreOrder) {
        org.springframework.test.web.client.MockRestServiceServer.MockRestServiceServerBuilder b =
                MockRestServiceServer.bindTo(builder);
        if (ignoreOrder) {
            b.ignoreExpectOrder(true);
        }
        this.mockBoundBuilder = builder;
        this.server = b.build();
        this.baseUrl = "http://localhost";
    }

    /**
     * 返回绑定到 mock 的 RestClient（推荐使用此方法获取测试 client）。
     * 直接调 {@code builder.build()} 即可（builder 已被 bindTo 修改为 mock-aware）。
     */
    public RestClient getMockBoundClient() {
        return mockBoundBuilder.build();
    }

    /**
     * 业务后端 ping 接口：返回 UP（健康）+ 数据库元信息。
     */
    public MockSqlForgeServer expectHealthUp() {
        server.expect(requestTo(baseUrl + "/sql/forge/api/database/metaDataDatabase"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"productName\":\"H2\",\"productVersion\":\"2.3.232\"}",
                        MediaType.APPLICATION_JSON));
        return this;
    }

    /**
     * 业务后端 ping 接口：返回 DOWN（不可达 / 异常）。
     */
    public MockSqlForgeServer expectHealthDown() {
        server.expect(requestTo(baseUrl + "/sql/forge/api/database/metaDataDatabase"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new java.net.ConnectException("Connection refused")));
        return this;
    }

    /**
     * 业务后端 ping 接口：返回 401（鉴权失败）。
     */
    public MockSqlForgeServer expectHealthUnauthorized() {
        server.expect(requestTo(baseUrl + "/sql/forge/api/database/metaDataDatabase"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));
        return this;
    }

    /**
     * 元数据：列出所有表。
     *
     * @param tables 表信息列表（每个 Map 含 tableCat/tableSchema/tableName/tableType/remarks）
     */
    public MockSqlForgeServer expectMetaDataTables(List<Map<String, Object>> tables) {
        server.expect(requestTo(baseUrl + "/sql/forge/api/database/metaDataTables"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(toJson(tables), MediaType.APPLICATION_JSON));
        return this;
    }

    /**
     * 元数据：单张表的完整定义（含列/PK/FK/indexes）。
     * <p>
     * 实际 URL 带 ?tableName=...&tableType=...&catalog=...&schema=...，用 {@link #expectUri} 忽略 query。
     * </p>
     *
     * @param tableDef 表定义 JSON 字符串（List 形式）
     */
    public MockSqlForgeServer expectMetaDataDefinitions(String tableDefJson) {
        expectUri("/sql/forge/api/database/metaDataDefinitions", HttpMethod.GET)
                .andRespond(withSuccess(tableDefJson, MediaType.APPLICATION_JSON));
        return this;
    }

    /**
     * 元数据定义：模拟后端 500 错误（用于 PR-7 友好化测试）。
     */
    public MockSqlForgeServer expectMetaDataDefinitions500(String errorBody) {
        expectUri("/sql/forge/api/database/metaDataDefinitions", HttpMethod.GET)
                .andRespond(withStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorBody));
        return this;
    }

    /**
     * 元数据：树形结构。
     */
    public MockSqlForgeServer expectMetaDataTree(String treeJson) {
        server.expect(requestTo(baseUrl + "/sql/forge/api/database/metaDataTree"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(treeJson, MediaType.APPLICATION_JSON));
        return this;
    }

    /**
     * 执行器列表。
     */
    public MockSqlForgeServer expectExecutorNames(List<String> names) {
        server.expect(requestTo(baseUrl + "/sql/forge/api/console/executorName"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(toJson(names), MediaType.APPLICATION_JSON));
        return this;
    }

    /**
     * JSON CRUD 任一方法（select / selectPage / insert / update / delete）。
     *
     * @param method       "select" / "selectPage" / "insert" / "update" / "delete"
     * @param table        表名（URL path 段，会按已编码匹配）
     * @param responseJson 响应 JSON 字符串
     */
    public MockSqlForgeServer expectJsonCrud(String method, String table, String responseJson) {
        server.expect(requestTo(baseUrl + "/sql/forge/api/json/" + method + "/" + table))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
        return this;
    }

    /**
     * JSON CRUD：模拟后端返回 400（请求参数错）。
     */
    public MockSqlForgeServer expectJsonCrudBadRequest(String method, String table, String errorBody) {
        server.expect(requestTo(baseUrl + "/sql/forge/api/json/" + method + "/" + table))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .method(org.springframework.http.HttpMethod.POST))
                .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_REQUEST).body(errorBody));
        return this;
    }

    /**
     * Amis 模板：列出所有模板（实际 URL 带 ?id=&name=&description=&context=，用 expectUri 忽略 query）。
     */
    public MockSqlForgeServer expectAmisTemplateList(String listJson) {
        expectUri("/sql/forge/api/template/amis", HttpMethod.GET)
                .andRespond(withSuccess(listJson, MediaType.APPLICATION_JSON));
        return this;
    }

    /**
     * Amis 模板：按 id 查询。
     */
    public MockSqlForgeServer expectAmisTemplateGet(String id, String templateJson) {
        // id 会被 encodePath 编码，所以 mock 也按编码后的路径匹配
        String encodedId = java.net.URLEncoder.encode(id, java.nio.charset.StandardCharsets.UTF_8);
        server.expect(requestTo(baseUrl + "/sql/forge/api/template/amis/" + encodedId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(templateJson, MediaType.APPLICATION_JSON));
        return this;
    }

    /**
     * Amis 模板：保存（PUT）。
     */
    public MockSqlForgeServer expectAmisTemplateSave(String responseBody) {
        server.expect(requestTo(baseUrl + "/sql/forge/api/template/amis"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
        return this;
    }

    /**
     * Amis 模板：删除（DELETE）。
     */
    public MockSqlForgeServer expectAmisTemplateDelete(String id) {
        String encodedId = java.net.URLEncoder.encode(id, java.nio.charset.StandardCharsets.UTF_8);
        server.expect(requestTo(baseUrl + "/sql/forge/api/template/amis/" + encodedId))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess("true", MediaType.APPLICATION_JSON));
        return this;
    }

    /**
     * SQL 模板：列出（实际 URL 带 ?id=&executorName=&context=，用 expectUri 忽略 query）。
     */
    public MockSqlForgeServer expectSqlTemplateList(String listJson) {
        expectUri("/sql/forge/api/template/sql", HttpMethod.GET)
                .andRespond(withSuccess(listJson, MediaType.APPLICATION_JSON));
        return this;
    }

    /**
     * SQL 模板：按 id 查询。
     */
    public MockSqlForgeServer expectSqlTemplateGet(String id, String templateJson) {
        String encodedId = java.net.URLEncoder.encode(id, java.nio.charset.StandardCharsets.UTF_8);
        server.expect(requestTo(baseUrl + "/sql/forge/api/template/sql/" + encodedId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(templateJson, MediaType.APPLICATION_JSON));
        return this;
    }

    /**
     * SQL 模板：保存（PUT）。
     */
    public MockSqlForgeServer expectSqlTemplateSave(String responseBody) {
        server.expect(requestTo(baseUrl + "/sql/forge/api/template/sql"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
        return this;
    }

    /**
     * SQL 模板：删除。
     */
    public MockSqlForgeServer expectSqlTemplateDelete(String id) {
        String encodedId = java.net.URLEncoder.encode(id, java.nio.charset.StandardCharsets.UTF_8);
        server.expect(requestTo(baseUrl + "/sql/forge/api/template/sql/" + encodedId))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess("true", MediaType.APPLICATION_JSON));
        return this;
    }

    /**
     * SQL 模板：执行（POST /template/sql/{id}）。
     */
    public MockSqlForgeServer expectSqlTemplateExecute(String id, String responseJson) {
        String encodedId = java.net.URLEncoder.encode(id, java.nio.charset.StandardCharsets.UTF_8);
        server.expect(requestTo(baseUrl + "/sql/forge/api/template/sql/" + encodedId))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
        return this;
    }

    /**
     * 直接执行 SQL（POST /execute）。
     */
    public MockSqlForgeServer expectExecuteSql(String responseJson) {
        server.expect(requestTo(baseUrl + "/sql/forge/api/database/execute"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
        return this;
    }

    /**
     * 通用 mock 入口：透传 {@link MockRestServiceServer#expect} 给高级用例（如自定义错误码）。
     */
    public org.springframework.test.web.client.ResponseActions expectRequest(
            org.springframework.test.web.client.RequestMatcher matcher) throws java.io.IOException {
        return server.expect(matcher);
    }

    /**
     * 忽略 query string 的 URL matcher —— 处理 SQL CRUD / Template 列表接口带 query 参数的场景。
     *
     * @param pathWithoutQuery 不含 query 的 URL path（如 {@code /sql/forge/api/json/select/users}）
     * @param httpMethod      HTTP method
     * @return ResponseActions，可继续链式 .andRespond()
     */
    public org.springframework.test.web.client.ResponseActions expectUri(
            String pathWithoutQuery, HttpMethod httpMethod) {
        String expectedPath = java.net.URI.create(baseUrl + pathWithoutQuery).getPath();
        return server.expect(new org.springframework.test.web.client.RequestMatcher() {
            @Override
            public void match(org.springframework.http.client.ClientHttpRequest request) throws java.io.IOException {
                org.springframework.test.web.client.match.MockRestRequestMatchers
                        .method(httpMethod).match(request);
                String actualPath = request.getURI().getPath();
                if (!actualPath.equals(expectedPath)) {
                    throw new AssertionError(
                            "URI path mismatch: expected=" + expectedPath + " actual=" + actualPath);
                }
            }
        });
    }

    /**
     * 验证所有预期都被调用（可选，JUnit 自动断言）。
     */
    public void verify() {
        server.verify();
    }

    /**
     * 重置 mock（清掉所有预期，重新设置）。
     */
    public void reset() {
        server.reset();
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize mock response", e);
        }
    }
}
