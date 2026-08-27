package cn.wubo.sql.forge;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * sql-forge-mcp 业务 Tool 共用辅助类：系统上下文、错误映射、URL 编码、body 大小校验。
 * <p>
 * 拆分前的 {@link SqlForgeMcpService} 单类 700 行，现在按领域拆为 5 个 Service：
 * </p>
 * <ul>
 *   <li>{@link MetadataService} —— 元数据查询（8 个 Tool）</li>
 *   <li>{@link JsonCrudService} —— CRUD + executeSQL（6 个 Tool）</li>
 *   <li>{@link TemplateService} —— 模板 CRUD + SQL 模板执行（10 个 Tool）</li>
 *   <li>{@link AmisService} —— 校验 / 渲染 / 健康检查（3 个 Tool）</li>
 *   <li>{@link SqlForgeMcpService} —— metrics 入口 + 委托（1 个 Tool）</li>
 * </ul>
 * <p>
 * 所有 Service 共享本类提供的：
 * </p>
 * <ul>
 *   <li>{@link #withCtx} —— 系统上下文包装（带指标 + 审计 + 异常映射）</li>
 *   <li>{@link #resolveSystem} —— systemName O(1) 查找</li>
 *   <li>{@link #encodePath} —— URL path 段编码</li>
 *   <li>{@link #checkBodySize} —— 请求体大小校验</li>
 *   <li>{@link #friendlyClientError} / {@link #friendlyServerError} —— 异常→友好字符串</li>
 * </ul>
 */
public class McpToolSupport {

    private final List<SqlForgeMcpProperties.SystemInfo> systems;
    private final Map<String, SqlForgeMcpProperties.SystemInfo> systemByName;
    private final MetricsService metricsService;
    private final AuditLogger auditLogger;
    private final int maxRequestBytes;
    private final int maxResponseBytes;

    public McpToolSupport(List<SqlForgeMcpProperties.SystemInfo> systems,
                          MetricsService metricsService,
                          AuditLogger auditLogger,
                          int maxRequestBytes,
                          int maxResponseBytes) {
        this.systems = systems;
        this.systemByName = new java.util.HashMap<>();
        for (SqlForgeMcpProperties.SystemInfo s : systems) {
            this.systemByName.putIfAbsent(s.getName(), s);
        }
        this.metricsService = metricsService;
        this.auditLogger = auditLogger;
        this.maxRequestBytes = maxRequestBytes;
        this.maxResponseBytes = maxResponseBytes;
    }

    // ====================== 系统上下文 ======================

    /**
     * 远程系统调用上下文（baseUrl + apiKey）。
     */
    public record SystemContext(String baseUrl, String apiKey) {
    }

    /**
     * 把系统名称解析为远程调用上下文，找不到时返回错误字符串。
     */
    public Object resolveSystem(String systemName) {
        SqlForgeMcpProperties.SystemInfo info = systemByName.get(systemName);
        if (info == null) {
            return "系统不存在: " + systemName;
        }
        return new SystemContext(info.getUrl(), info.getApiKey());
    }

    /**
     * 在系统上下文中执行操作：含错误映射 + 指标 + 审计。
     */
    public <T> Object withCtx(String systemName, Function<SystemContext, T> action) {
        Object resolved = resolveSystem(systemName);
        if (resolved instanceof String err) {
            return err;
        }
        SystemContext ctx = (SystemContext) resolved;
        long startNs = System.nanoTime();
        boolean success = false;
        Object result = null;
        try {
            result = action.apply(ctx);
            success = true;
            return result;
        } catch (HttpClientErrorException ex) {
            result = friendlyClientError(ex, ctx);
            return result;
        } catch (HttpServerErrorException ex) {
            result = friendlyServerError(ex, ctx);
            return result;
        } catch (ResourceAccessException ex) {
            result = "无法连接后端 " + ctx.baseUrl() + "（可能是连接拒绝/超时/DNS 失败）："
                    + ex.getMostSpecificCause().getMessage();
            return result;
        } catch (RuntimeException ex) {
            result = "调用后端失败（" + ex.getClass().getSimpleName() + "）：" + ex.getMessage();
            return result;
        } finally {
            long latencyMs = (System.nanoTime() - startNs) / 1_000_000;
            metricsService.recordCall(systemName, latencyMs, success);
            auditLogger.log(systemName, systemName, Map.of(), result, success, latencyMs);
        }
    }

    // ====================== 错误映射 ======================

    private String friendlyClientError(HttpClientErrorException ex, SystemContext ctx) {
        int status = ex.getStatusCode().value();
        return switch (status) {
            case 401 -> "认证失败：后端 " + ctx.baseUrl() + " 拒绝 X-Api-Key，请检查 sql.forge.mcp.systems[].apiKey 配置";
            case 403 -> "权限不足：" + ex.getStatusText() + "，请联系后端管理员检查该 apiKey 对应的权限";
            case 404 -> "后端路径不存在：HTTP 404 — 请确认 " + ctx.baseUrl() + " 是 SQL Forge 服务且版本匹配";
            case 400 -> "请求被后端拒绝（400）：" + truncate(ex.getResponseBodyAsString(), 200);
            default -> "客户端错误 HTTP " + status + "：" + truncate(ex.getResponseBodyAsString(), 200);
        };
    }

    private String friendlyServerError(HttpServerErrorException ex, SystemContext ctx) {
        return "后端 " + ctx.baseUrl() + " 内部错误 HTTP " + ex.getStatusCode().value()
                + "：" + truncate(ex.getResponseBodyAsString(), 200)
                + "（请稍后重试或联系后端管理员）";
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "(空响应体)";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    // ====================== URL 编码 ======================

    /**
     * 对 URL path 段做 RFC 3986 编码（URLEncoder.encode 实现）。
     */
    public static String encodePath(String segment) {
        if (segment == null || segment.isEmpty()) return "";
        return URLEncoder.encode(segment, StandardCharsets.UTF_8);
    }

    // ====================== 请求体大小校验 ======================

    /**
     * 校验请求体大小，超限抛 {@link IllegalArgumentException}（由 {@code withCtx} 兜底转成友好消息）。
     */
    public void checkBodySize(Map<String, Object> body, String methodName) {
        if (body == null || body.isEmpty()) return;
        long estimated = 16L * body.size();
        for (Object v : body.values()) {
            if (v == null) {
                estimated += 4;
            } else if (v instanceof String s) {
                estimated += 32 + s.length() * 2;
            } else if (v instanceof Number || v instanceof Boolean) {
                estimated += 24;
            } else if (v instanceof java.util.Collection<?> coll) {
                estimated += 24L * coll.size();
            } else if (v instanceof Map<?, ?> map) {
                estimated += 32L * map.size();
            } else {
                estimated += 64;
            }
        }
        if (estimated > maxRequestBytes) {
            throw new IllegalArgumentException(String.format(
                    "请求体过大（估算 %d 字节，上限 %d 字节）。请减小 @set / @where / @column 中的数据量后重试。",
                    estimated, maxRequestBytes));
        }
    }

    public int getMaxRequestBytes() {
        return maxRequestBytes;
    }

    public int getMaxResponseBytes() {
        return maxResponseBytes;
    }
}
