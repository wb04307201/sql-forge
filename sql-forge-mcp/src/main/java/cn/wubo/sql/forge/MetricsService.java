package cn.wubo.sql.forge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * sql-forge-mcp 进程内指标收集器。
 * <p>
 * 设计原则：
 * <ul>
 *   <li>不引入 Micrometer/Actuator —— sql-forge-mcp 是 stdio MCP server，无 HTTP，
 *       加 Actuator 反而引入 50MB+ 依赖</li>
 *   <li>用 {@link LongAdder} 做线程安全计数器（高并发写性能优于 {@code AtomicLong}）</li>
 *   <li>通过 {@code @Tool metrics()} 暴露给 AI Agent / 运维排查</li>
 * </ul>
 * <p>
 * 跟踪指标：
 * </p>
 * <ul>
 *   <li>{@code mcp_tool_calls_total{tool,status}} —— 调用次数（status=ok/error）</li>
 *   <li>{@code mcp_tool_latency_ms_total{tool}} —— 累计延迟（ms）</li>
 *   <li>{@code mcp_tool_started_at} —— 进程启动时间戳</li>
 * </ul>
 */
public class MetricsService {

    /** 每个工具的指标：tool 名 → 指标 */
    private final ConcurrentMap<String, ToolMetrics> toolMetrics = new ConcurrentHashMap<>();
    private final long startedAtMs;

    public MetricsService() {
        this.startedAtMs = System.currentTimeMillis();
    }

    /**
     * 记录一次 Tool 调用结果。
     *
     * @param toolName  Tool 方法名
     * @param latencyMs 本次调用延迟（毫秒）
     * @param success   true=成功/false=失败
     */
    public void recordCall(String toolName, long latencyMs, boolean success) {
        toolMetrics.computeIfAbsent(toolName, k -> new ToolMetrics()).record(latencyMs, success);
    }

    /**
     * 导出当前所有指标的快照（用于 {@code @Tool metrics()} 返回）。
     * <p>
     * 输出 JSON 结构：
     * </p>
     * <pre>
     * {
     *   "startedAt": "2026-08-26T10:00:00Z",
     *   "uptimeSec": 3600,
     *   "tools": {
     *     "jsonSelect": {
     *       "calls": 1234,
     *       "errors": 5,
     *       "avgLatencyMs": 12.3,
     *       "p95EstimateMs": 45
     *     },
     *     ...
     *   }
     * }
     * </pre>
     */
    public Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("startedAtMs", startedAtMs);
        out.put("uptimeSec", (System.currentTimeMillis() - startedAtMs) / 1000);

        Map<String, Object> tools = new LinkedHashMap<>();
        for (Map.Entry<String, ToolMetrics> e : toolMetrics.entrySet()) {
            ToolMetrics tm = e.getValue();
            long calls = tm.calls.sum();
            long errors = tm.errors.sum();
            long totalMs = tm.totalLatencyMs.sum();
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("calls", calls);
            t.put("errors", errors);
            t.put("errorRate", calls == 0 ? 0.0 : (double) errors / calls);
            t.put("avgLatencyMs", calls == 0 ? 0.0 : (double) totalMs / calls);
            // P95 估计：简单用 max latency 作下界（精确 P95 需要 reservoir sampling）
            t.put("maxLatencyMs", tm.maxLatencyMs.get());
            tools.put(e.getKey(), t);
        }
        out.put("tools", tools);
        return out;
    }

    /**
     * 单个 Tool 的指标容器（线程安全）。
     */
    private static class ToolMetrics {
        final LongAdder calls = new LongAdder();
        final LongAdder errors = new LongAdder();
        final LongAdder totalLatencyMs = new LongAdder();
        final java.util.concurrent.atomic.AtomicLong maxLatencyMs = new java.util.concurrent.atomic.AtomicLong(0);

        void record(long latencyMs, boolean success) {
            calls.increment();
            totalLatencyMs.add(latencyMs);
            // 更新 max（无锁 CAS）
            long current;
            do {
                current = maxLatencyMs.get();
                if (latencyMs <= current) break;
            } while (!maxLatencyMs.compareAndSet(current, latencyMs));
            if (!success) errors.increment();
        }
    }
}
