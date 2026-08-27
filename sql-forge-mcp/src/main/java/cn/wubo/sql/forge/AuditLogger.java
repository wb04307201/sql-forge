package cn.wubo.sql.forge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * sql-forge-mcp 审计日志：每个 Tool 调用持久化一条 JSON 行。
 * <p>
 * 设计原则：
 * </p>
 * <ul>
 *   <li>独立文件（{@code ./mcp/audit.log}），便于运维 grep / 归档 / 合规审计</li>
 *   <li>每行一条 JSON（JSON Lines 格式），方便 awk / jq / ELK 解析</li>
 *   <li>args 和 result 字段截断到 1KB（防大 payload 把日志撑爆）</li>
 *   <li>失败不抛异常（写日志失败不能让 Tool 调用失败）</li>
 * </ul>
 * <p>
 * 字段：
 * </p>
 * <pre>{@code
 * {
 *   "ts": "2026-08-26T10:00:00.123Z",
 *   "tool": "jsonSelect",
 *   "systemName": "TestSys",
 *   "args": { ... },            // 截断到 1KB
 *   "success": true,
 *   "latencyMs": 23,
 *   "error": null               // 或错误消息
 * }
 * }</pre>
 */
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);
    private static final int MAX_FIELD_BYTES = 1024;

    private final Path logFile;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile boolean disabled = false;

    /**
     * 默认写到 {@code ./mcp/audit.log}。
     */
    public AuditLogger() {
        this(Path.of("./mcp/audit.log"));
    }

    /**
     * 指定路径（用于测试）。
     */
    public AuditLogger(Path logFile) {
        this.logFile = logFile;
        try {
            Files.createDirectories(logFile.getParent());
            if (!Files.exists(logFile)) {
                Files.createFile(logFile);
            }
        } catch (IOException e) {
            // 写不了文件不抛异常，让 Tool 调用继续（降级为仅 SLF4J）
            log.warn("审计日志文件创建失败 {}: {}", logFile, e.getMessage());
            disabled = true;
        }
    }

    /**
     * 记录一次 Tool 调用。
     *
     * @param toolName   Tool 方法名
     * @param systemName 系统名（可能 null，如 mcpHealth）
     * @param args       Tool 入参
     * @param result     Tool 返回值
     * @param success    是否成功
     * @param latencyMs  延迟（毫秒）
     */
    public void log(String toolName, String systemName, Map<String, Object> args,
                    Object result, boolean success, long latencyMs) {
        if (disabled) {
            return;
        }
        try {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("ts", Instant.now().toString());
            entry.put("tool", toolName);
            entry.put("systemName", systemName);
            entry.put("args", truncate(args));
            entry.put("success", success);
            entry.put("latencyMs", latencyMs);
            entry.put("error", success ? null : truncateString(result));

            byte[] line = (mapper.writeValueAsString(entry) + "\n").getBytes(StandardCharsets.UTF_8);
            // append 模式（追加写）+ CREATE 不加（已存在）
            Files.write(logFile, line,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            // 写日志失败不能让 Tool 调用失败
            log.warn("审计日志写入失败: {}", e.getMessage());
        }
    }

    /**
     * 截断对象为 Map（>1KB 转为 { "_truncated": true, "_size": N }）。
     */
    private Object truncate(Object o) {
        if (o == null) return null;
        try {
            String json = mapper.writeValueAsString(o);
            if (json.length() <= MAX_FIELD_BYTES) {
                return mapper.readValue(json, Object.class);
            }
            // 截断
            Map<String, Object> marker = new LinkedHashMap<>();
            marker.put("_truncated", true);
            marker.put("_sizeBytes", json.length());
            return marker;
        } catch (Exception e) {
            return truncateString(o);
        }
    }

    private String truncateString(Object o) {
        if (o == null) return null;
        String s = o.toString();
        return s.length() <= MAX_FIELD_BYTES ? s : s.substring(0, MAX_FIELD_BYTES) + "...";
    }

    /**
     * 读取所有审计日志行（仅测试用）。
     */
    public java.util.List<String> readAll() throws IOException {
        if (!Files.exists(logFile)) return java.util.Collections.emptyList();
        return Files.readAllLines(logFile, StandardCharsets.UTF_8);
    }
}
