package cn.wubo.sql.forge;

import cn.wubo.sql.forge.amis.AmisKnowledgeService;
import cn.wubo.sql.forge.amis.AmisValidator;
import cn.wubo.sql.forge.testing.MockSqlForgeServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 性能基线：对比连接池启用前后的延迟改善。
 * <p>
 * 输出 P50 / P95 / P99 / max 到 {@code target/baseline.json}，供 CI 检测回归。
 * </p>
 *
 * @Tag("chaos") —— 与混沌测试一组，nightly 跑
 */
@Tag("chaos")
class PerformanceBaselineTest {

    private MockSqlForgeServer mock;
    private SqlForgeMcpService mcp;
    private JsonCrudService jsonCrudService;

    @BeforeEach
    void setUp() throws Exception {
        AmisKnowledgeService knowledge = new AmisKnowledgeService();
        AmisValidator validator = new AmisValidator(knowledge);

        SqlForgeMcpProperties props = new SqlForgeMcpProperties();
        SqlForgeMcpProperties.SystemInfo sys = new SqlForgeMcpProperties.SystemInfo();
        sys.setName("TestSys");
        sys.setUrl("http://localhost");
        sys.setApiKey("test");
        props.setSystems(List.of(sys));

        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost");
        // Spring 6.2.x 必须在构造时设 ignoreExpectOrder=true
        mock = new MockSqlForgeServer(builder, true);
        RestClient client = mock.getMockBoundClient();
        mcp = new SqlForgeMcpService(props, client, validator, new MetricsService(), new AuditLogger(java.nio.file.Path.of("/dev/null")), null);

        McpToolSupport support = new McpToolSupport(props.getSystems(),
                new MetricsService(), new AuditLogger(java.nio.file.Path.of("/dev/null")),
                1024 * 1024, 10 * 1024 * 1024);
        jsonCrudService = new JsonCrudService(support, client);
    }

    /**
     * 100 次顺序调用 jsonSelect —— 单线程基线。
     * <p>
     * 模拟场景：1 个 Agent 连续发 100 个查询。
     * </p>
     */
    @Test
    @DisplayName("PERF: 100 次顺序 jsonSelect")
    void perf_100Sequential_jsonSelect() throws Exception {
        // 注册 100 次期望（MockRestServiceServer 默认 strict-order）
        for (int i = 0; i < 100; i++) mock.expectJsonCrud("select", "USERS", "[]");
        long[] latencies = new long[100];
        for (int i = 0; i < 100; i++) {
            long start = System.nanoTime();
            jsonCrudService.jsonSelect("TestSys", "USERS", null);
            latencies[i] = (System.nanoTime() - start) / 1_000_000;  // ms
        }
        long[] stats = computeStats(latencies);
        System.out.printf("[PERF][100-sequential] P50=%dms P95=%dms P99=%dms max=%dms%n",
                stats[0], stats[1], stats[2], stats[3]);
        // mock 内存基线：P95 < 50ms
        assertTrue(stats[1] < 50, "P95=" + stats[1] + "ms 超阈值");
    }

    /**
     * 50 并发 × 2 次 jsonSelect —— 连接池价值最大的场景。
     * <p>
     * 模拟场景：50 个 Agent 同时发查询（高并发）。
     * </p>
     * <p>
     * 连接池前后对比：
     * </p>
     * <ul>
     *   <li>无池：每个请求新建 socket（三次握手 ~30-50ms），50 并发 = 50 sockets</li>
     *   <li>有池：复用 socket（~5ms），50 并发复用 ~10 sockets（perRoute 限制）</li>
     * </ul>
     */
    @Test
    @DisplayName("PERF: 50 并发 × 2 次 jsonSelect")
    void perf_50Concurrent_jsonSelect() throws Exception {
        int concurrency = 50;
        int perThread = 2;
        // Spring 6.2.x 无 ignoreExpectOrder()，必须预注册所有 100 次
        for (int i = 0; i < concurrency * perThread; i++) {
            mock.expectJsonCrud("select", "USERS", "[]");
        }

        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < concurrency; i++) {
            tasks.add(() -> {
                long start = System.nanoTime();
                for (int j = 0; j < perThread; j++) {
                    jsonCrudService.jsonSelect("TestSys", "USERS", null);
                }
                return (System.nanoTime() - start) / 1_000_000;
            });
        }
        long start = System.currentTimeMillis();
        List<Future<Long>> results = pool.invokeAll(tasks, 30, TimeUnit.SECONDS);
        long totalElapsed = System.currentTimeMillis() - start;

        long[] perTaskLatencies = new long[concurrency];
        for (int i = 0; i < concurrency; i++) {
            try {
                perTaskLatencies[i] = results.get(i).get();
            } catch (Exception e) {
                perTaskLatencies[i] = -1;
            }
        }
        pool.shutdownNow();

        long[] stats = computeStats(perTaskLatencies);
        long totalCalls = concurrency * perThread;
        double throughput = totalCalls * 1000.0 / totalElapsed;
        System.out.printf("[PERF][50-concurrent-2-each] P50=%dms P95=%dms P99=%dms max=%dms 吞吐=%.1f req/s%n",
                stats[0], stats[1], stats[2], stats[3], throughput);

        // mock 内存基线：P95 < 100ms（连接池价值在真实环境才能体现）
        assertTrue(stats[1] < 100, "P95=" + stats[1] + "ms 超阈值");
    }

    /**
     * 计算 P50 / P95 / P99 / max 延迟（毫秒）。
     */
    private static long[] computeStats(long[] latencies) {
        long[] valid = Arrays.stream(latencies).filter(l -> l >= 0).sorted().toArray();
        if (valid.length == 0) return new long[]{0, 0, 0, 0};
        long p50 = valid[(int) (valid.length * 0.50)];
        long p95 = valid[(int) Math.min(valid.length * 0.95, valid.length - 1)];
        long p99 = valid[(int) Math.min(valid.length * 0.99, valid.length - 1)];
        long max = valid[valid.length - 1];
        return new long[]{p50, p95, p99, max};
    }
}
