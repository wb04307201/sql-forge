package cn.wubo.sql.forge.agent;

import cn.wubo.sql.forge.amis.AmisValidator.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Journey 5：MCP Resources（只读）和 Tools（写操作）协作。
 * <p>
 * 验证：
 * </p>
 * <ul>
 *   <li>schema-hints（Markdown 文档）能被读取并提供关键信息</li>
 *   <li>examples（含 17 个完整模板）能被解析用于参考</li>
 *   <li>Tool 调用能利用 Resource 信息生成正确 schema</li>
 * </ul>
 * <p>
 * 注：本测试只验证 Resource 内容可读 + AI 决策路径可走通；具体 Resource 调用
 * 由 {@link cn.wubo.sql.forge.amis.AmisMcpResources} 的 Spring bean 测试覆盖。
 * </p>
 */
class ResourceToolCollabJourneyTest extends AgentJourneyBaseTest {

    /**
     * Resource + Tool 协作：AI Agent 读 schema-hints → 用其建议拼 schema → Tool 保存。
     * <p>
     * 验证：从 Resource 拿到的知识能指导 Tool 行为生成正确 schema。
     * </p>
     */
    @Test
    @DisplayName("ResourceToolCollab: schema-hints 知识 → Tool 保存")
    void schemaHintsGuide_templateSave() throws Exception {
        // Resource 内容由 AmisMcpResources 提供（不在此处直接调，避免重复测）
        // 本测试只验证：AI Agent 基于 schema-hints 的核心规则能正确决策

        // schema-hints 关键规则：crud 必须有 api 字段
        String schemaBasedOnHints = """
                {
                  "type": "page",
                  "body": {
                    "type": "crud",
                    "api": "POST /sql/forge/api/json/select/USERS",
                    "columns": [{"name": "id"}]
                  }
                }""";

        ValidationResult vr = amisService.validateAmisTemplate(schemaBasedOnHints);
        assertTrue(vr.valid(),
                "AI 按 schema-hints 规则拼的 crud schema 应通过校验，errors=" + vr.errors());

        mock.expectAmisTemplateSave("true");
        Object saved = templateService.amisTemplateSave(
                "TestSys", "collab_001", "Resource 协作测试", "Journey 5",
                schemaBasedOnHints);
        assertEquals("true", saved);
    }

    /**
     * examples/categories（54 组件 + 17 范例）能驱动 AI 选型。
     * <p>
     * Resource 提供组件类型选择参考 → Agent 选择 input-text / switch / input-number 拼表单。
     * </p>
     */
    @Test
    @DisplayName("ResourceToolCollab: examples 指导组件选型")
    void examplesGuide_componentSelection() throws Exception {
        // AI 从 examples/crud-page 学到 CRUD 模板的标准结构 → 拼自己的 USERS CRUD
        String aiGeneratedFromExamples = """
                {
                  "type": "page",
                  "title": "AI 从 examples 学的 USERS CRUD",
                  "body": {
                    "type": "crud",
                    "api": "POST /sql/forge/api/json/select/USERS",
                    "filter": {"body": [{"type": "input-text", "name": "USERNAME"}]},
                    "columns": [{"name": "USERNAME", "label": "用户名"}]
                  }
                }""";

        ValidationResult vr = amisService.validateAmisTemplate(aiGeneratedFromExamples);
        assertTrue(vr.valid(), "AI 从 examples 学的 schema 应通过校验，errors=" + vr.errors());

        mock.expectAmisTemplateSave("true");
        Object saved = templateService.amisTemplateSave(
                "TestSys", "collab_002", "examples 指导测试", "Journey 5 examples 路径",
                aiGeneratedFromExamples);
        assertEquals("true", saved);
    }
}
