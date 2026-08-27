package cn.wubo.sql.forge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuditLogger 单测：覆盖 JSON Lines 写入 / args 截断 / 读回。
 */
class AuditLoggerTest {

    @Test
    void log_writesJsonLine(@TempDir Path tmp) throws Exception {
        Path logFile = tmp.resolve("audit.log");
        AuditLogger logger = new AuditLogger(logFile);
        logger.log("jsonSelect", "TestSys", Map.of("tableName", "USERS"),
                "[{\"ID\":\"1\"}]", true, 23);
        logger.log("mcpHealth", null, Map.of(),
                "{\"status\":\"UP\"}", true, 5);

        List<String> lines = logger.readAll();
        assertEquals(2, lines.size());
        // 每行都是合法 JSON
        for (String line : lines) {
            assertTrue(line.startsWith("{") && line.endsWith("}"));
            assertTrue(line.contains("\"tool\":"));
            assertTrue(line.contains("\"ts\":"));
        }
    }

    @Test
    void log_truncatesOversizedArgs(@TempDir Path tmp) throws Exception {
        Path logFile = tmp.resolve("audit.log");
        AuditLogger logger = new AuditLogger(logFile);
        // 构造一个 > 1KB 的 args
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 2000; i++) huge.append("x");
        logger.log("jsonInsert", "TestSys",
                Map.of("payload", huge.toString()),
                "ok", true, 10);

        List<String> lines = logger.readAll();
        assertEquals(1, lines.size());
        // 应包含 _truncated 标记
        assertTrue(lines.get(0).contains("_truncated"));
        assertTrue(lines.get(0).contains("_sizeBytes"));
    }

    @Test
    void log_failureIncludesError(@TempDir Path tmp) throws Exception {
        Path logFile = tmp.resolve("audit.log");
        AuditLogger logger = new AuditLogger(logFile);
        logger.log("jsonSelect", "TestSys", Map.of(),
                "后端 500 错误", false, 100);

        List<String> lines = logger.readAll();
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("\"success\":false"));
        assertTrue(lines.get(0).contains("后端 500 错误"));
    }

    @Test
    void constructor_handlesReadOnlyDir(@TempDir Path tmp) throws Exception {
        // 模拟 /dev/null 或 readonly 目录：构造不应抛异常
        Path readonlyDir = tmp.resolve("readonly");
        Files.createDirectory(readonlyDir);
        // 设为只读
        readonlyDir.toFile().setReadOnly();
        Path logFile = readonlyDir.resolve("audit.log");

        // 不应抛异常（应降级为 disabled 模式）
        AuditLogger logger = new AuditLogger(logFile);
        // 调用也不应抛
        logger.log("tool", null, Map.of(), "ok", true, 1);
        // 文件可能没创建（read-only），但 logger 应不抛
    }
}
