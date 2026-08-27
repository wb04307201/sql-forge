package cn.wubo.sql.forge.agent;

import cn.wubo.sql.forge.JsonCrudService;
import cn.wubo.sql.forge.TemplateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Journey 4：危险 Tool 守卫（防误删）。
 * <p>
 * 验证：
 * </p>
 * <ul>
 *   <li>3 个危险 Tool 的 description 含 ⚠️ DESTRUCTIVE: 前缀</li>
 *   <li>误调危险 Tool 时后端返回友好错误（不静默删全表）</li>
 * </ul>
 */
class DestructiveGuardTest extends AgentJourneyBaseTest {

    /** 应含 ⚠️ DESTRUCTIVE: 前缀的 Tool 名称列表。 */
    private static final List<String> DESTRUCTIVE_TOOL_NAMES = Arrays.asList(
            "jsonDelete",         // JsonCrudService
            "deleteAmisTemplate", // TemplateService
            "deleteSqlTemplate"); // TemplateService

    /**
     * 反射验证 3 个危险 Tool 的 @Tool 描述含 ⚠️ DESTRUCTIVE: 前缀。
     * <p>
     * AI Agent 在看到 description 时会自动识别危险操作并向用户确认（取决于 MCP 客户端实现）。
     * </p>
     */
    @Test
    @DisplayName("Dangerous: 3 个危险 Tool 描述含 ⚠️ DESTRUCTIVE: 前缀")
    void destructiveTool_descriptions_have_warning() throws Exception {
        // 检查 JsonCrudService.jsonDelete
        Method jsonDelete = JsonCrudService.class.getMethod("jsonDelete",
                String.class, String.class, java.util.Map.class);
        Tool tool1 = jsonDelete.getAnnotation(Tool.class);
        assertNotNull(tool1, "jsonDelete 必须有 @Tool 注解");
        assertTrue(tool1.description().contains("⚠️ DESTRUCTIVE"),
                "jsonDelete 描述必须含 ⚠️ DESTRUCTIVE: 实际=" + tool1.description());
        assertTrue(tool1.description().toLowerCase().contains("delete"),
                "jsonDelete 描述应提到 'delete'");

        // 检查 TemplateService.deleteAmisTemplate
        Method delAmis = TemplateService.class.getMethod("deleteAmisTemplate",
                String.class, String.class);
        Tool tool2 = delAmis.getAnnotation(Tool.class);
        assertNotNull(tool2);
        assertTrue(tool2.description().contains("⚠️ DESTRUCTIVE"),
                "deleteAmisTemplate 描述必须含 ⚠️ DESTRUCTIVE: 实际=" + tool2.description());

        // 检查 TemplateService.deleteSqlTemplate
        Method delSql = TemplateService.class.getMethod("deleteSqlTemplate",
                String.class, String.class);
        Tool tool3 = delSql.getAnnotation(Tool.class);
        assertNotNull(tool3);
        assertTrue(tool3.description().contains("⚠️ DESTRUCTIVE"),
                "deleteSqlTemplate 描述必须含 ⚠️ DESTRUCTIVE: 实际=" + tool3.description());
    }

    /**
     * 误调 jsonDelete（后端 404）→ 友好错误，不抛 stack trace。
     * <p>
     * 验证 mcp 的 {@code withCtx} 异常映射（4xx → 友好字符串）。
     * </p>
     */
    @Test
    @DisplayName("Dangerous: jsonDelete 后端 404 → 友好错误")
    void jsonDelete_backend404_returnsFriendlyError() {
        // mock 后端返回 404
        mock.expectJsonCrudBadRequest("delete", "USERS",
                "{\"error\":\"table not found\"}");

        Object r = jsonCrudService.jsonDelete("TestSys", "USERS", java.util.Map.of());
        assertNotNull(r);
        // 应返回 String 而非抛异常
        assertTrue(r instanceof String, "应返回 String 而非异常，实际=" + r.getClass());
        // 错误消息含 "后端路径不存在" 或 "请求被后端拒绝"
        String msg = r.toString();
        assertTrue(msg.contains("后端") || msg.contains("404") || msg.contains("路径"),
                "应含友好错误提示，实际=" + msg);
    }
}
