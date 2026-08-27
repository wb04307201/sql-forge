package cn.wubo.sql.forge;

import cn.wubo.sql.forge.amis.AmisKnowledgeService;
import cn.wubo.sql.forge.amis.AmisValidator;
import cn.wubo.sql.forge.testing.MockSqlForgeServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * sql-forge-mcp 混沌测试 + 性能基线 + 内存稳定性。
 * <p>
 * 在 CI 上跑（~30s），验证：
 * <ul>
 *   <li>混沌：后端挂掉 → 重启 → MCP 工具恢复</li>
 *   <li>混沌：10 个并发调用全部成功</li>
 *   <li>混沌：慢响应（模拟 1s 延迟）→ 不死锁 + 正确返回</li>
 *   <li>性能：100 次 mcpHealth 的 P95 &lt; 100ms（基线）</li>
 *   <li>内存：1000 次 CRUD 调用后 heap 增长 &lt; 50MB</li>
 * </ul>
 *
 * @Tag("chaos") —— 慢测试（包含慢响应模拟 1s），PR 跑集成时跳过，nightly 必跑
 */
@Tag("chaos")
class ChaosAndPerformanceTest {

    private static final String BASE_URL = "http://localhost";
    private MockSqlForgeServer mock;
    private SqlForgeMcpService mcp;
    private MetadataService metadataService;
    private TemplateService templateService;
    private AmisService amisService;
    private JsonCrudService jsonCrudService;

    @BeforeEach
    void setUp() throws Exception {
        AmisKnowledgeService knowledge = new AmisKnowledgeService();
        AmisValidator validator = new AmisValidator(knowledge);

        SqlForgeMcpProperties props = new SqlForgeMcpProperties();
        SqlForgeMcpProperties.SystemInfo sys = new SqlForgeMcpProperties.SystemInfo();
        sys.setName("TestSys");
        sys.setUrl(BASE_URL);
        sys.setApiKey("test");
        props.setSystems(List.of(sys));

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mock = new MockSqlForgeServer(builder);
        mcp = new SqlForgeMcpService(props, builder.build(), validator, new MetricsService(), new AuditLogger(java.nio.file.Path.of("/dev/null")), null);
        McpToolSupport support = new McpToolSupport(props.getSystems(),
                new MetricsService(), new AuditLogger(java.nio.file.Path.of("/dev/null")),
                1024 * 1024, 10 * 1024 * 1024);
        metadataService = new MetadataService(support, builder.build());
        templateService = new TemplateService(support, builder.build());
        amisService = new AmisService(support, validator, null);
        jsonCrudService = new JsonCrudService(support, builder.build());
    }

    // ====================== 混沌测试 ======================

    @Nested
    @DisplayName("混沌测试")
    class ChaosTests {

        /**
         * CHA-1: 后端 503 → 工具返回友好错误
         */
        @Test
        @DisplayName("CHA-1: 后端 503 → 友好错误")
        void backend503_friendlyError() throws Exception {
            mock.expectHealthUp();
            mock.expectRequest(requestTo(BASE_URL + "/sql/forge/api/database/metaDataTables"))
                    .andExpect(method(org.springframework.http.HttpMethod.GET))
                    .andRespond(withStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                            .body("{\"error\":\"maintenance\"}"));

            // 第 1 次 ping：OK
            Map<String, Object> h = mcp.mcpHealth();
            assertEquals("UP", h.get("status"));

            // 第 2 次调表：503 → 友好错误
            Object r = metadataService.sqlForgeMetaDataTables("TestSys");
            assertNotNull(r);
            assertTrue(r.toString().contains("503"));
        }

        /**
         * CHA-2: 10 个并发调用全部成功
         */
        @Test
        @DisplayName("CHA-2: 10 个并发 mcpHealth 调用全部成功")
        void concurrent10_AllSucceed() throws Exception {
            // 一次性设好 10 次期望
            for (int i = 0; i < 10; i++) mock.expectHealthUp();

            ExecutorService pool = Executors.newFixedThreadPool(10);
            try {
                List<Callable<String>> tasks = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    tasks.add(() -> {
                        Map<String, Object> h = mcp.mcpHealth();
                        return (String) h.get("status");
                    });
                }
                List<Future<String>> results = pool.invokeAll(tasks, 30, TimeUnit.SECONDS);
                for (Future<String> f : results) {
                    assertEquals("UP", f.get(), "每个并发调用都应返回 UP");
                }
            } finally {
                pool.shutdownNow();
            }
        }

        /**
         * CHA-3: 后端慢响应（人为 sleep 1s）→ 不死锁 + 返回结果
         * <p>
         * 注：真实的 readTimeout 超时（10s）必须在真实环境测（test-mcp-e2e.sh），
         * 因为 mock 同步阻塞调用线程无法触发 socket 超时。
         * </p>
         */
        @Test
        @DisplayName("CHA-3: 后端慢响应 → 不死锁 + 返回结果")
        void backendSlow_10sTimeout() throws Exception {
            // mock 模拟慢响应（1s，远小于 readTimeout 10s）
            mock.expectRequest(requestTo(BASE_URL + "/sql/forge/api/database/metaDataTables"))
                    .andExpect(method(org.springframework.http.HttpMethod.GET))
                    .andRespond((org.springframework.test.web.client.ResponseCreator) request -> {
                        try {
                            Thread.sleep(1_000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return withSuccess("[{\"tableName\":\"SLOW\"}]", MediaType.APPLICATION_JSON).createResponse(request);
                    });

            long start = System.currentTimeMillis();
            Object r = metadataService.sqlForgeMetaDataTables("TestSys");
            long elapsed = System.currentTimeMillis() - start;

            // 应在 ~1s 后返回（不卡死）
            assertTrue(elapsed >= 1_000, "应等满 1s，实际 " + elapsed + "ms");
            assertTrue(elapsed < 3_000, "不应阻塞超过 3s，实际 " + elapsed + "ms");
            // 返回结果包含 SLOW
            assertNotNull(r);
            assertTrue(r.toString().contains("SLOW"));
        }

        /**
         * CHA-4: 后端连接拒绝（TCP RST）→ 友好错误
         */
        @Test
        @DisplayName("CHA-4: 连接拒绝 → 友好错误")
        void connectionRefused_friendlyError() throws Exception {
            mock.expectHealthUp();
            mock.expectRequest(requestTo(BASE_URL + "/sql/forge/api/database/metaDataTables"))
                    .andExpect(method(org.springframework.http.HttpMethod.GET))
                    .andRespond((org.springframework.test.web.client.ResponseCreator) request ->
                            withException(new java.net.ConnectException("Connection refused")).createResponse(request));

            // 第 1 次：UP
            Map<String, Object> h = mcp.mcpHealth();
            assertEquals("UP", h.get("status"));

            // 第 2 次：连接拒绝
            Object r = metadataService.sqlForgeMetaDataTables("TestSys");
            assertNotNull(r);
            assertTrue(r.toString().contains("无法连接"));
        }
    }

    // ====================== 性能基线 ======================

    @Nested
    @DisplayName("性能基线")
    class PerformanceBaseline {

        /**
         * PERF-1: 100 次 mcpHealth 的 P95 延迟 < 50ms（mock 内存计算）
         * <p>
         * mock server 无 I/O，所以阈值严格：P95 < 50ms。
         * 若此基线退化，说明 Service 内部逻辑变慢（如多了一次 Map 查表、一次字符串拼接）。
         * 真实环境 P95 应 &lt; 100ms（test-mcp-e2e.sh 测）。
         * </p>
         */
        @Test
        @DisplayName("PERF-1: 100 次 mcpHealth P95 < 50ms")
        void mcpHealth_p95_under_50ms() {
            // 一次性设好 100 次期望
            for (int i = 0; i < 100; i++) mock.expectHealthUp();

            long[] latencies = new long[100];
            for (int i = 0; i < 100; i++) {
                long start = System.nanoTime();
                mcp.mcpHealth();
                latencies[i] = (System.nanoTime() - start) / 1_000_000;  // ms
            }
            Arrays.sort(latencies);
            long p50 = latencies[50];
            long p95 = latencies[95];
            long p99 = latencies[99];
            long max = latencies[99];

            // 性能门禁：P95 < 50ms（mock 内存基线）
            System.out.printf("[PERF] mcpHealth 100 calls: P50=%dms, P95=%dms, P99=%dms, max=%dms%n",
                    p50, p95, p99, max);
            assertTrue(p95 < 50,
                    "PERF-GATE FAILED: mcpHealth P95=" + p95 + "ms (阈值 50ms)");
        }

        /**
         * PERF-2: 100 次 jsonSelect 的 P95 延迟 < 50ms
         */
        @Test
        @DisplayName("PERF-2: 100 次 jsonSelect P95 < 50ms")
        void jsonSelect_p95_under_50ms() {
            for (int i = 0; i < 100; i++) {
                mock.expectJsonCrud("select", "USERS", "[]");
            }

            long[] latencies = new long[100];
            for (int i = 0; i < 100; i++) {
                long start = System.nanoTime();
                jsonCrudService.jsonSelect("TestSys", "USERS", null);
                latencies[i] = (System.nanoTime() - start) / 1_000_000;
            }
            Arrays.sort(latencies);
            long p95 = latencies[95];
            long p99 = latencies[99];
            System.out.printf("[PERF] jsonSelect 100 calls: P95=%dms, P99=%dms%n", p95, p99);
            assertTrue(p95 < 50,
                    "PERF-GATE FAILED: jsonSelect P95=" + p95 + "ms (阈值 50ms)");
        }
    }

    // ====================== 内存稳定性 ======================

    @Nested
    @DisplayName("内存稳定性")
    class MemoryStability {

        /**
         * MEM-1: 1000 次 CRUD 调用后 heap 增长 < 50MB
         */
        @Test
        @DisplayName("MEM-1: 1000 次 CRUD heap 增长 < 50MB")
        void thousandCrudCalls_heapGrowth_under_50mb() {
            Runtime rt = Runtime.getRuntime();
            // 主动 GC 后测基线
            System.gc();
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            long baselineUsed = rt.totalMemory() - rt.freeMemory();

            // 一次性设好 1000 次期望
            for (int i = 0; i < 1000; i++) {
                mock.expectJsonCrud("select", "USERS", "[]");
            }

            // 跑 1000 次
            for (int i = 0; i < 1000; i++) {
                jsonCrudService.jsonSelect("TestSys", "USERS", null);
            }

            // GC 后再测
            System.gc();
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            long finalUsed = rt.totalMemory() - rt.freeMemory();
            long deltaMb = (finalUsed - baselineUsed) / (1024 * 1024);

            System.out.printf("1000 calls: baseline=%dMB, final=%dMB, delta=%dMB%n",
                    baselineUsed / (1024 * 1024),
                    finalUsed / (1024 * 1024),
                    deltaMb);

            // 增长应 < 50MB（实际应 < 5MB，这里放宽以减少误报）
            assertTrue(deltaMb < 50, "1000 次调用后 heap 增长应 < 50MB，实际 " + deltaMb + "MB");
        }

        /**
         * MEM-2: 混合调用（CRUD + Amis + Resource proxy）1000 次 heap 稳定
         */
        @Test
        @DisplayName("MEM-2: 1000 次混合调用 heap 增长 < 50MB")
        void mixedCalls_heapGrowth_under_50mb() {
            Runtime rt = Runtime.getRuntime();
            System.gc();
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            long baselineUsed = rt.totalMemory() - rt.freeMemory();

            // 混合调用期望
            for (int i = 0; i < 500; i++) mock.expectJsonCrud("select", "USERS", "[]");
            for (int i = 0; i < 300; i++) mock.expectHealthUp();
            for (int i = 0; i < 200; i++) mock.expectAmisTemplateGet("t_" + i, "{}");

            // 混合跑
            for (int i = 0; i < 500; i++) jsonCrudService.jsonSelect("TestSys", "USERS", null);
            for (int i = 0; i < 300; i++) mcp.mcpHealth();
            for (int i = 0; i < 200; i++) templateService.getAmisTemplate("TestSys", "t_" + i);

            System.gc();
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            long finalUsed = rt.totalMemory() - rt.freeMemory();
            long deltaMb = (finalUsed - baselineUsed) / (1024 * 1024);

            System.out.printf("1000 mixed calls: baseline=%dMB, final=%dMB, delta=%dMB%n",
                    baselineUsed / (1024 * 1024),
                    finalUsed / (1024 * 1024),
                    deltaMb);
            assertTrue(deltaMb < 50, "1000 次混合调用 heap 增长应 < 50MB，实际 " + deltaMb + "MB");
        }
    }
}

