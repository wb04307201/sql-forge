package cn.wubo.sql.forge.amis;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AmisMcpPrompts 测试：验证 3 个 Prompt 模板能被正确实例化、参数能解析、消息结构合理。
 * <p>
 * 通过反射取出 {@link McpServerFeatures.SyncPromptSpecification} 的 prompt 字段和 handler，
 * 直接调用 handler（mock exchange），检查返回的 {@link McpSchema.GetPromptResult}。
 * </p>
 *
 * @Tag("fast") —— 单测，PR 必跑
 */
@Tag("fast")
class AmisMcpPromptsTest {

    private AmisMcpPrompts prompts;
    private List<McpServerFeatures.SyncPromptSpecification> specs;

    @BeforeEach
    void setUp() {
        prompts = new AmisMcpPrompts();
        // 直接调 @Bean 方法拿到 3 个 SyncPromptSpecification
        specs = prompts.amisPrompts();
    }

    /**
     * 3 个 Prompt 全部注册。
     */
    @Test
    @DisplayName("3 个 Prompt 全部注册")
    void threePromptsRegistered() {
        assertEquals(3, specs.size());
        assertEquals("create-amis-page", getPromptName(0));
        assertEquals("diagnose-render-error", getPromptName(1));
        assertEquals("quick-crud-template", getPromptName(2));
    }

    @Nested
    @DisplayName("Prompt 1: create-amis-page")
    class CreateAmisPagePrompt {

        @Test
        @DisplayName("参数完整 → 返回引导消息含 6 步")
        void fullArgs_returnsCompleteGuide() {
            Map<String, Object> args = Map.of(
                    "systemName", "TestSys",
                    "tableName", "PRODUCTS",
                    "pageTitle", "商品管理");
            McpSchema.GetPromptResult result = invokePrompt(0, args);

            assertNotNull(result);
            assertNotNull(result.description());
            // 至少包含 2 条消息：assistant 引导 + user 任务
            assertTrue(result.messages().size() >= 2);

            // 拼接所有消息文本检查关键步骤
            String allText = result.messages().stream()
                    .map(McpSchema.PromptMessage::content)
                    .filter(c -> c instanceof McpSchema.TextContent)
                    .map(c -> ((McpSchema.TextContent) c).text())
                    .reduce("", (a, b) -> a + "\n" + b);

            // 应包含 6 步引导
            assertTrue(allText.contains("Step 1"), "应包含 Step 1: " + allText.substring(0, 200));
            assertTrue(allText.contains("Step 6"), "应包含 Step 6");
            // 应包含表名和 pageTitle
            assertTrue(allText.contains("PRODUCTS"));
            assertTrue(allText.contains("商品管理"));
            // 应提示调用 Tool
            assertTrue(allText.contains("getMetaDataTableInfo"));
            assertTrue(allText.contains("validateAmisTemplate"));
            assertTrue(allText.contains("previewAmisTemplate"));
            assertTrue(allText.contains("amisTemplateSave"));
        }

        @Test
        @DisplayName("参数缺失 → 使用默认值不抛")
        void missingArgs_usesDefaults() {
            McpSchema.GetPromptResult result = invokePrompt(0, Map.of());
            assertNotNull(result);
            // 不抛异常，能拿到引导消息
            assertFalse(result.messages().isEmpty());
        }
    }

    @Nested
    @DisplayName("Prompt 2: diagnose-render-error")
    class DiagnoseRenderErrorPrompt {

        @Test
        @DisplayName("提供 context + errors → 返回修复建议")
        void withErrors_returnsFixSuggestions() {
            String fakeContext = "{\"type\":\"page\",\"title\":\"x\"}";
            String fakeErrors = "[\"network error\", \"console error\"]";

            McpSchema.GetPromptResult result = invokePrompt(1, Map.of(
                    "context", fakeContext,
                    "errors", fakeErrors));

            assertNotNull(result);
            String allText = messagesToText(result);
            // 应包含 4 步诊断流程
            assertTrue(allText.contains("错误分类"));
            assertTrue(allText.contains("修复建议") || allText.contains("诊断") || allText.contains("修复"));
            // 应包含错误案例表（network / console / CORS 等）
            assertTrue(allText.contains("network") || allText.contains("Connection"));
            // 应包含 context 引用
            assertTrue(allText.contains(fakeContext) || allText.contains("context"));
        }

        @Test
        @DisplayName("空 args → 不抛")
        void emptyArgs_noThrow() {
            McpSchema.GetPromptResult result = invokePrompt(1, Map.of());
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Prompt 3: quick-crud-template")
    class QuickCrudTemplatePrompt {

        @Test
        @DisplayName("带 systemName + tableName → 返回 CRUD 骨架")
        void withTableName_returnsCrudSkeleton() {
            McpSchema.GetPromptResult result = invokePrompt(2, Map.of(
                    "systemName", "TestSys",
                    "tableName", "ORDERS",
                    "withFilter", "true"));

            assertNotNull(result);
            String allText = messagesToText(result);
            // 应包含 3 步
            assertTrue(allText.contains("Step 1"));
            assertTrue(allText.contains("Step 2") || allText.contains("生成"));
            assertTrue(allText.contains("ORDERS"));
            // 应提到 CRUD 骨架
            assertTrue(allText.contains("crud") || allText.contains("CRUD"));
        }

        @Test
        @DisplayName("空 args → 不抛")
        void emptyArgs_noThrow() {
            McpSchema.GetPromptResult result = invokePrompt(2, Map.of());
            assertNotNull(result);
        }
    }

    // ============ 工具方法 ============

    /**
     * 反射取出第 index 个 SyncPromptSpecification，调用其 handler。
     */
    @SuppressWarnings("unchecked")
    private McpSchema.GetPromptResult invokePrompt(int index, Map<String, Object> args) {
        try {
            McpServerFeatures.SyncPromptSpecification spec = specs.get(index);
            Field handlerField = McpServerFeatures.SyncPromptSpecification.class
                    .getDeclaredField("promptHandler");
            handlerField.setAccessible(true);
            BiFunction<Object, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> handler =
                    (BiFunction<Object, McpSchema.GetPromptRequest, McpSchema.GetPromptResult>) handlerField.get(spec);

            McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest(
                    "test-id", args);
            // exchange 传 null —— handler 内部不需要 exchange（只读 args）
            return handler.apply(null, request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke prompt " + index, e);
        }
    }

    private String getPromptName(int index) {
        try {
            McpServerFeatures.SyncPromptSpecification spec = specs.get(index);
            Field promptField = McpServerFeatures.SyncPromptSpecification.class
                    .getDeclaredField("prompt");
            promptField.setAccessible(true);
            return ((McpSchema.Prompt) promptField.get(spec)).name();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String messagesToText(McpSchema.GetPromptResult result) {
        return result.messages().stream()
                .map(McpSchema.PromptMessage::content)
                .filter(c -> c instanceof McpSchema.TextContent)
                .map(c -> ((McpSchema.TextContent) c).text())
                .reduce("", (a, b) -> a + "\n" + b);
    }
}
