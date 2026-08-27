package cn.wubo.sql.forge.agent;

import cn.wubo.sql.forge.amis.AmisValidator.ValidationError;
import cn.wubo.sql.forge.amis.AmisValidator.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Journey 2：修复循环（错误反馈 → 修复 → 重新校验）。
 * <p>
 * 模拟 AI Agent 的"看到错误 → 修改 schema → 重新校验"循环，
 * 验证修复循环能在有限步骤内收敛到正确 schema。
 * </p>
 */
class FixLoopJourneyTest extends AgentJourneyBaseTest {

    /**
     * 验证错误修复循环最终能收敛到通过校验。
     * <p>
     * 起点：crud 缺 api（validator 必报错：'缺少必填字段: api'）
     * 终点：完整 schema（type=page + body.type=crud + body.api）
     * </p>
     */
    @Test
    @DisplayName("FixLoop: crud 缺 api 修复后能通过校验")
    void fixLoop_convergesAfterFix() {
        // === 起点 schema：crud 缺 api（必报错）===
        String schema = """
                {
                  "type": "page",
                  "body": {
                    "type": "crud",
                    "columns": [{"name": "id"}]
                  }
                }""";

        ValidationResult r0 = amisService.validateAmisTemplate(schema);
        assertFalse(r0.valid(), "起点（crud 缺 api）应报错");
        List<String> history = new ArrayList<>();
        history.add("r0: " + r0.errors().get(0).message());

        // === 第 1 轮修复：加 api ===
        schema = """
                {
                  "type": "page",
                  "body": {
                    "type": "crud",
                    "api": "POST /sql/forge/api/json/select/USERS",
                    "columns": [{"name": "id"}]
                  }
                }""";
        ValidationResult r1 = amisService.validateAmisTemplate(schema);
        assertTrue(r1.valid(), "修复后应通过：history=" + history + " r1.errors=" + r1.errors());
        history.add("r1: PASS");

        // === 验证：保存修复后的 schema ===
        mock.expectAmisTemplateSave("true");
        Object saved = templateService.amisTemplateSave(
                "TestSys", "fixloop_final", "修复循环结果", "Journey 2",
                schema);
        assertEquals("true", saved);
    }

    /**
     * 校验错误信息含具体 path（让 Agent 能定位修复哪里）。
     */
    @Test
    @DisplayName("FixLoop: errors 含具体 path 而不只是根 '$'")
    void fixLoop_errorsHavePath() {
        String badSchema = """
                {
                  "type": "page",
                  "body": {"type": "crud", "columns": [{"name": "id"}]}
                }""";

        ValidationResult r = amisService.validateAmisTemplate(badSchema);
        assertFalse(r.valid(), "缺 api 应报错");

        // 至少 1 个 error 含 path 指向 body.api
        boolean hasNestedPath = r.errors().stream()
                .anyMatch(e -> e.path() != null && e.path().contains("body.api"));
        assertTrue(hasNestedPath, "errors 应定位到 body.api，errors=" + r.errors());
    }
}
