package cn.wubo.sql.forge.amis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AmisValidator 单测：覆盖合法 / 非法 / 必填缺失 / API 格式 / 嵌套递归 等 10+ 用例。
 *
 * @Tag("fast") —— 纯单元测试，PR 必跑
 */
@Tag("fast")
class AmisValidatorTest {

    private AmisValidator validator;

    @BeforeEach
    void setUp() throws IOException {
        AmisKnowledgeService knowledge = new AmisKnowledgeService();
        validator = new AmisValidator(knowledge);
    }

    @Test
    void shouldValidateLegalCrud() {
        String json = """
                {
                  "type": "page",
                  "title": "用户管理",
                  "body": {
                    "type": "crud",
                    "api": "POST /sql/forge/api/json/select/users",
                    "columns": [{"name": "id", "label": "ID"}]
                  }
                }
                """;
        AmisValidator.ValidationResult result = validator.validate(json);
        assertTrue(result.valid(), "合法 CRUD 应该校验通过，errors=" + result.errors());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void shouldRejectEmptyContext() {
        AmisValidator.ValidationResult result = validator.validate("");
        assertFalse(result.valid());
        assertEquals(1, result.errors().size());
        assertEquals("模板内容为空", result.errors().get(0).message());
    }

    @Test
    void shouldRejectNullContext() {
        AmisValidator.ValidationResult result = validator.validate(null);
        assertFalse(result.valid());
        assertEquals("error", result.errors().get(0).severity());
    }

    @Test
    void shouldRejectBrokenJson() {
        AmisValidator.ValidationResult result = validator.validate("{not json}");
        assertFalse(result.valid());
        assertEquals("error", result.errors().get(0).severity());
        assertTrue(result.errors().get(0).message().contains("JSON 解析失败"));
    }

    @Test
    void shouldRejectCrudMissingApi() {
        String json = """
                {
                  "type": "page",
                  "body": {
                    "type": "crud",
                    "columns": [{"name": "id", "label": "ID"}]
                  }
                }
                """;
        AmisValidator.ValidationResult result = validator.validate(json);
        assertFalse(result.valid());
        boolean found = result.errors().stream()
                .anyMatch(e -> "缺少必填字段: api".equals(e.message()) && e.path().endsWith(".api"));
        assertTrue(found, "应报告 crud 缺少 api");
    }

    @Test
    void shouldRejectFormMissingApi() {
        String json = """
                {
                  "type": "page",
                  "body": {
                    "type": "form",
                    "body": []
                  }
                }
                """;
        AmisValidator.ValidationResult result = validator.validate(json);
        assertFalse(result.valid());
        boolean found = result.errors().stream()
                .anyMatch(e -> "缺少必填字段: api".equals(e.message()) && e.path().endsWith(".api"));
        assertTrue(found);
    }

    @Test
    void shouldWarnForUnknownComponentType() {
        String json = """
                {
                  "type": "page",
                  "body": {
                    "type": "super-duper-unknown-thing",
                    "columns": []
                  }
                }
                """;
        AmisValidator.ValidationResult result = validator.validate(json);
        boolean warned = result.errors().stream()
                .anyMatch(e -> "warning".equals(e.severity()) && e.message().contains("super-duper-unknown-thing"));
        assertTrue(warned, "未知组件应产生 warning");
    }

    @Test
    void shouldWarnForBadApiFormat() {
        String json = """
                {
                  "type": "page",
                  "body": {
                    "type": "crud",
                    "api": "/sql/forge/api/json/select/users",
                    "columns": []
                  }
                }
                """;
        AmisValidator.ValidationResult result = validator.validate(json);
        boolean warned = result.errors().stream()
                .anyMatch(e -> e.message().contains("api 格式不规范"));
        assertTrue(warned);
    }

    @Test
    void shouldRecurseIntoFormBody() {
        String json = """
                {
                  "type": "page",
                  "body": {
                    "type": "form",
                    "api": "POST /sql/forge/api/json/insert/users",
                    "body": [
                      {"type": "input-text", "name": "name"}
                    ]
                  }
                }
                """;
        AmisValidator.ValidationResult result = validator.validate(json);
        assertTrue(result.valid(), "form 嵌套合法子项应通过，errors=" + result.errors());
    }

    @Test
    void shouldRecurseIntoCrudColumns() {
        String json = """
                {
                  "type": "page",
                  "body": {
                    "type": "crud",
                    "api": "POST /sql/forge/api/json/select/users",
                    "columns": [
                      {"name": "id", "label": "ID"},
                      {"name": "name", "label": "姓名"}
                    ]
                  }
                }
                """;
        AmisValidator.ValidationResult result = validator.validate(json);
        assertTrue(result.valid(), "crud columns 递归应通过，errors=" + result.errors());
    }

    @Test
    void shouldRecurseIntoDialogBody() {
        String json = """
                {
                  "type": "page",
                  "body": {
                    "type": "button",
                    "label": "新增",
                    "actionType": "dialog",
                    "dialog": {
                      "title": "新增",
                      "body": {
                        "type": "form",
                        "api": "POST /sql/forge/api/json/insert/users",
                        "body": []
                      }
                    }
                  }
                }
                """;
        AmisValidator.ValidationResult result = validator.validate(json);
        assertTrue(result.valid(), "dialog → form 嵌套应通过，errors=" + result.errors());
    }

    @Test
    void shouldRecurseIntoTabs() {
        String json = """
                {
                  "type": "page",
                  "body": {
                    "type": "tabs",
                    "tabs": [
                      {"title": "Tab1", "body": {"type": "panel", "body": "内容"}},
                      {"title": "Tab2", "body": {"type": "table", "dataSource": "${rows}", "columns": []}}
                    ]
                  }
                }
                """;
        AmisValidator.ValidationResult result = validator.validate(json);
        assertTrue(result.valid(), "tabs 嵌套应通过，errors=" + result.errors());
    }

    @Test
    void shouldRecurseIntoWizardSteps() {
        String json = """
                {
                  "type": "page",
                  "body": {
                    "type": "wizard",
                    "steps": [
                      {"title": "Step1", "body": {"type": "form", "api": "POST /api/x", "body": []}}
                    ]
                  }
                }
                """;
        AmisValidator.ValidationResult result = validator.validate(json);
        assertTrue(result.valid(), "wizard 嵌套应通过，errors=" + result.errors());
    }

    @Test
    void shouldAcceptBuiltinTypeWithoutWarning() {
        // built-in 'tpl' is in BUILTIN_TYPES, should not warn
        String json = """
                {
                  "type": "page",
                  "body": {
                    "type": "tpl",
                    "tpl": "hello"
                  }
                }
                """;
        AmisValidator.ValidationResult result = validator.validate(json);
        boolean hasTypeWarn = result.errors().stream()
                .anyMatch(e -> "warning".equals(e.severity()) && e.message().contains("tpl"));
        assertFalse(hasTypeWarn, "内置类型 tpl 不应产生 type warning");
    }

    @Test
    void shouldRejectRootNotObject() {
        AmisValidator.ValidationResult result = validator.validate("[1,2,3]");
        assertFalse(result.valid());
        assertEquals("error", result.errors().get(0).severity());
        assertTrue(result.errors().get(0).message().contains("对象"));
    }
}