package cn.wubo.sql.forge;

import cn.wubo.sql.forge.amis.PlaywrightRenderer;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * sql-forge-mcp 顶层 facade：保留 mcpHealth + metrics 两个 Tool，其余全部委托给领域 Service。
 * <p>
 * 拆分架构（Round 5）：
 * </p>
 * <ul>
 *   <li>{@link McpToolSupport} —— 系统上下文 / 错误映射 / URL 编码 / 大小校验（共享）</li>
 *   <li>{@link MetadataService} —— 8 个元数据 Tool</li>
 *   <li>{@link JsonCrudService} —— 6 个 CRUD + executeSQL Tool</li>
 *   <li>{@link TemplateService} —— 10 个模板 CRUD + SQL 模板 Tool</li>
 *   <li>{@link AmisService} —— 2 个校验/渲染 Tool</li>
 *   <li>{@link SqlForgeMcpService} —— 本类：1 个 mcpHealth + 1 个 metrics</li>
 * </ul>
 */
public class SqlForgeMcpService {

    private final McpToolSupport support;
    private final RestClient restClient;
    private final MetricsService metricsService;
    private final List<SqlForgeMcpProperties.SystemInfo> systems;
    private final PlaywrightRenderer amisRenderer;
    private final int maxRequestBytes;
    private final int maxResponseBytes;

    /**
     * 构造 facade。
     */
    public SqlForgeMcpService(SqlForgeMcpProperties properties,
                              RestClient restClient,
                              cn.wubo.sql.forge.amis.AmisValidator amisValidator,
                              MetricsService metricsService,
                              AuditLogger auditLogger,
                              PlaywrightRenderer amisRenderer) {
        this.systems = properties.getSystems();
        this.metricsService = metricsService != null ? metricsService : new MetricsService();
        this.amisRenderer = amisRenderer;
        this.maxRequestBytes = properties.getMaxRequestBytes();
        this.maxResponseBytes = properties.getMaxResponseBytes();
        // 共享支持：处理 system 查找 + 错误映射 + URL 编码 + 大小校验 + 指标 + 审计
        this.support = new McpToolSupport(systems, this.metricsService,
                auditLogger != null ? auditLogger : new AuditLogger(java.nio.file.Path.of("/dev/null")),
                maxRequestBytes, maxResponseBytes);
        this.restClient = restClient;
        // AmisValidator 必须非 null 才能 validate AmIS 模板
        // （不在本 facade 使用，留作后续扩展）
    }

    /**
     * 获取已注册系统列表（兼容方法，非 @Tool）。
     */
    public List<SqlForgeMcpProperties.SystemInfo> getSystems() {
        return systems;
    }

    /**
     * 健康检查 Tool：探测每个 systemName 后端的可达性 + Playwright 可用性。
     */
    @Tool(description = "MCP server + 各业务后端 + Playwright 的健康检查。"
            + "返回结构化 JSON 包含 overall/mcp/backends/playwright/limits 字段。"
            + "用于探活、AI Agent 启动自检、人工排查。")
    public Map<String, Object> mcpHealth() {
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> mcpInfo = new LinkedHashMap<>();
        mcpInfo.put("status", "UP");
        mcpInfo.put("registeredTools", 29);
        mcpInfo.put("registeredResources", 3);
        mcpInfo.put("registeredPrompts", 3);
        mcpInfo.put("registeredResourceTemplates", 2);
        mcpInfo.put("systemCount", systems.size());
        mcpInfo.put("knowledgeCatalogComponents", 54);
        mcpInfo.put("knowledgeExamples", 17);
        result.put("mcp", mcpInfo);

        // 各 systemName 后端可达性
        Map<String, Object> backends = new LinkedHashMap<>();
        boolean allUp = true;
        for (SqlForgeMcpProperties.SystemInfo sys : systems) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("url", sys.getUrl());
            long start = System.currentTimeMillis();
            try {
                restClient.get()
                        .uri(sys.getUrl() + cn.wubo.sql.forge.Constant.GET_METADATA_DATABASE_URL)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Api-Key", sys.getApiKey())
                        .retrieve()
                        .body(String.class);
                entry.put("status", "UP");
            } catch (Exception ex) {
                allUp = false;
                entry.put("status", "DOWN");
                entry.put("error", ex.getClass().getSimpleName() + ": " +
                        (ex.getMessage() == null ? "" : ex.getMessage().substring(0, Math.min(100, ex.getMessage().length()))));
            }
            entry.put("latencyMs", System.currentTimeMillis() - start);
            backends.put(sys.getName(), entry);
        }
        result.put("backends", backends);

        // Playwright 状态
        Map<String, Object> pw = new LinkedHashMap<>();
        if (amisRenderer == null) {
            pw.put("status", "DISABLED");
            pw.put("reason", "Playwright 不在 classpath 或未注册 bean（previewAmisTemplate 返回 available=false）");
        } else {
            pw.put("status", "AVAILABLE");
            pw.put("note", "首次调用 previewAmisTemplate 时会懒启动 Chromium（10s 超时）");
        }
        result.put("playwright", pw);

        // 限制配置
        Map<String, Object> limits = new LinkedHashMap<>();
        limits.put("maxRequestBytes", maxRequestBytes);
        limits.put("maxResponseBytes", maxResponseBytes);
        result.put("limits", limits);

        result.put("status", allUp ? "UP" : "DEGRADED");
        result.put("checkedAt", java.time.Instant.now().toString());
        return result;
    }

    /**
     * 进程内指标：每个 Tool 的调用次数 / 错误数 / 平均延迟 / 最大延迟。
     */
    @Tool(description = "进程内指标：每个 Tool 的调用次数、错误数、平均延迟、最大延迟。"
            + "返回结构化 JSON，用于探活、告警、Agent 自我诊断。")
    public Map<String, Object> metrics() {
        return metricsService.snapshot();
    }

    // ============ 兼容构造（向后兼容 / 单测） ============

    /**
     * 兼容构造：单测场景下用默认 MetricsService + AuditLogger（/dev/null）+ 默认大小限制。
     */
    public SqlForgeMcpService(List<SqlForgeMcpProperties.SystemInfo> systems,
                              RestClient restClient,
                              cn.wubo.sql.forge.amis.AmisValidator amisValidator,
                              PlaywrightRenderer amisRenderer) {
        SqlForgeMcpProperties props = new SqlForgeMcpProperties();
        props.setSystems(systems);
        MetricsService m = new MetricsService();
        AuditLogger a = new AuditLogger(java.nio.file.Path.of("/dev/null"));
        this.systems = systems;
        this.metricsService = m;
        this.amisRenderer = amisRenderer;
        this.maxRequestBytes = props.getMaxRequestBytes();
        this.maxResponseBytes = props.getMaxResponseBytes();
        this.support = new McpToolSupport(systems, m, a, maxRequestBytes, maxResponseBytes);
        this.restClient = restClient;
    }

    /**
     * 兼容构造（仅系统+HTTP）。
     */
    public SqlForgeMcpService(List<SqlForgeMcpProperties.SystemInfo> systems, RestClient restClient) {
        this(systems, restClient, null, null);
    }
}
