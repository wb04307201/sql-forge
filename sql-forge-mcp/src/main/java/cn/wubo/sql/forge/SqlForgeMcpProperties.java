package cn.wubo.sql.forge;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "sql.forge.mcp")
public class SqlForgeMcpProperties {

    private List<SystemInfo> systems = new ArrayList<>();

    /**
     * MCP 进程发给后端的请求体最大字节数（防 OOM / 防 Agent 误发巨大 payload）。
     * <p>
     * 超限立即抛 {@link IllegalArgumentException}（由 {@code withCtx} 兜底转成友好消息），
     * 不发到后端。默认 1 MiB，覆盖绝大多数 CRUD 场景。
     * </p>
     */
    private int maxRequestBytes = 1024 * 1024;

    /**
     * MCP 进程从后端读取的响应体最大字节数（防后端返回巨大结果集导致 OOM）。
     * <p>
     * 通过 {@code Content-Length} 预检 + {@code SimpleClientHttpRequestFactory} 的 readTimeout
     * 双重保护。超限立即抛 {@link IllegalStateException}。默认 10 MiB。
     * </p>
     */
    private int maxResponseBytes = 10 * 1024 * 1024;

    /**
     * 各 Tool 的 read 超时（毫秒）。key = Tool 方法名（如 "jsonSelect"），value = 超时。
     * <p>
     * 未配置的 Tool 用 {@link #defaultReadTimeoutMs}。例：
     * </p>
     * <pre>{@code
     * sql.forge.mcp.tool-timeouts:
     *   jsonSelect: 5000
     *   previewAmisTemplate: 30000
     *   getMetaDataDatabase: 3000
     * }</pre>
     */
    private java.util.Map<String, Integer> toolTimeouts = new java.util.HashMap<>();

    /**
     * Tool 默认 read 超时（毫秒）。单个 Tool 可通过 {@link #toolTimeouts} 覆盖。
     */
    private int defaultReadTimeoutMs = 10_000;

    @Data
    public static class SystemInfo {
        private String name;
        private String url;
        private String description;
        private String apiKey;
    }

}
