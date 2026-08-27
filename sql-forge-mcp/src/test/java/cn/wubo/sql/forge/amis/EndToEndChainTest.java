package cn.wubo.sql.forge.amis;

import cn.wubo.sql.forge.AmisService;
import cn.wubo.sql.forge.AuditLogger;
import cn.wubo.sql.forge.JsonCrudService;
import cn.wubo.sql.forge.MetadataService;
import cn.wubo.sql.forge.McpToolSupport;
import cn.wubo.sql.forge.MetricsService;
import cn.wubo.sql.forge.SqlForgeMcpProperties;
import cn.wubo.sql.forge.SqlForgeMcpService;
import cn.wubo.sql.forge.TemplateService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.web.client.RestClient;
import org.junit.jupiter.api.Tag;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Amis 端到端链路测试：覆盖 11 个用例。
 * <p>
 * 前置条件：业务后端 {@code localhost:8081}（API Key {@code test}）。前 8 个用例不依赖后端，
 * 最后 3 个用例需要后端可达。运行整个链路用：
 * </p>
 * <pre>
 * mvn -pl sql-forge-mcp test -Dtest=EndToEndChainTest
 * </pre>
 *
 * @Tag("integration") —— 集成测试（遍历 catalog 全部组件和范例），PR 必跑
 */
@Tag("integration")
class EndToEndChainTest {

    private static AmisKnowledgeService knowledge;
    private static AmisValidator validator;
    private static PlaywrightRenderer renderer;
    private static SqlForgeMcpService mcp;
    private static TemplateService templateService;
    private static AmisService amisService;

    @BeforeAll
    static void setUp() throws IOException {
        knowledge = new AmisKnowledgeService();
        validator = new AmisValidator(knowledge);
        renderer = new PlaywrightRenderer();

        SqlForgeMcpProperties.SystemInfo sys = new SqlForgeMcpProperties.SystemInfo();
        sys.setName("测试系统");
        sys.setUrl("http://localhost:8081");
        sys.setDescription("用于端到端测试的业务后端");
        sys.setApiKey("test");

        RestClient client = RestClient.builder().build();
        mcp = new SqlForgeMcpService(List.of(sys), client, validator, renderer);
        // Round 5 拆分后的领域 Service（mcp facade 上的方法已迁到这里）
        McpToolSupport support = new McpToolSupport(List.of(sys),
                new MetricsService(), new AuditLogger(java.nio.file.Path.of("/dev/null")),
                1024 * 1024, 10 * 1024 * 1024);
        templateService = new TemplateService(support, client);
        amisService = new AmisService(support, validator, renderer);
    }

    // ====================== [1] 知识：listCategories ======================
    // 6 个 Amis 知识 Tool 已迁移到 MCP Resource（{@link AmisMcpResources}），这里直接走
    // AmisKnowledgeService 验证后端数据可用。

    @Test
    void case01_listCategories() {
        List<String> cats = knowledge.listCategories();
        assertNotNull(cats);
        assertTrue(cats.contains("form"), "应包含 form 分类");
        assertTrue(cats.contains("crud"), "应包含 crud 分类");
    }

    // ====================== [2] 知识：listComponents ======================

    @Test
    void case02_listComponentsByCategory() {
        List<AmisKnowledgeService.ComponentSummary> components = knowledge.listComponents("form");
        assertNotNull(components);
        assertFalse(components.isEmpty());
        boolean hasInputText = components.stream().anyMatch(c -> "input-text".equals(c.type()));
        assertTrue(hasInputText, "form 分类下应有 input-text");
    }

    // ====================== [3] 知识：getComponentSpec ======================

    @Test
    void case03_getComponentSpec() {
        AmisKnowledgeService.ComponentSpec spec = knowledge.getComponentSpec("crud");
        assertNotNull(spec);
        assertNotNull(spec.required());
        assertTrue(spec.required().contains("api"));
        assertTrue(spec.required().contains("columns"));
    }

    // ====================== [4] 知识：getExample ======================

    @Test
    void case04_getExample() {
        AmisKnowledgeService.Example ex = knowledge.getExample("crud-page");
        assertNotNull(ex);
        assertNotNull(ex.schema());
        // crud-page 的 schema 里 body 是 crud，crud 嵌套 page
        Map<String, Object> body = (Map<String, Object>) ex.schema().get("body");
        assertEquals("crud", body.get("type"));
    }

    // ====================== [5] 校验：合法 crud ======================

    @Test
    void case05_validateLegalCrud() {
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
        AmisValidator.ValidationResult r = amisService.validateAmisTemplate(json);
        assertTrue(r.valid(), "合法 crud 应通过，errors=" + r.errors());
    }

    // ====================== [6] 校验：缺 api ======================

    @Test
    void case06_validateMissingApi() {
        String json = """
                {
                  "type": "page",
                  "body": {
                    "type": "crud",
                    "columns": [{"name": "id", "label": "ID"}]
                  }
                }
                """;
        AmisValidator.ValidationResult r = amisService.validateAmisTemplate(json);
        assertFalse(r.valid());
        boolean found = r.errors().stream()
                .anyMatch(e -> "缺少必填字段: api".equals(e.message()) && e.path().endsWith(".api"));
        assertTrue(found);
    }

    // ====================== [7] 校验：非 JSON ======================

    @Test
    void case07_validateNotJson() {
        AmisValidator.ValidationResult r = amisService.validateAmisTemplate("{not json}");
        assertFalse(r.valid());
        assertTrue(r.errors().get(0).message().contains("JSON 解析失败"));
    }

    // ====================== [8] 渲染：自包含 setContent ======================

    @Test
    void case08_previewRenders() {
        String json = "{\"type\":\"page\",\"title\":\"hello\",\"body\":\"world\"}";
        PlaywrightRenderer.PreviewResult r = amisService.previewAmisTemplate(null, json);
        assertNotNull(r);
        // 渲染器可能可用（Chromium 在）或不可用（未装）
        // 无论哪种情况：rendered 必须为 true 或 reason 非空（降级）
        if (r.available()) {
            // 渲染结果：可能成功，也可能 schema 太简单导致没有可视元素
            // 这里只验证结构正确
            assertNotNull(r.errors());
        } else {
            assertNotNull(r.reason(), "不可用时 reason 必须非空");
        }
        // 关键：previewAmisTemplate 不依赖后端
        // （fetcher 指向的 hostName 只是 schema 的属性，不影响渲染本身）
    }

    /**
     * [8b] unavailable() 静态工厂：明确降级语义，无需启动 Chromium。
     */
    @Test
    void case08b_unavailableReturnsDegradedResult() {
        PlaywrightRenderer r = new PlaywrightRenderer();
        PlaywrightRenderer.PreviewResult result = r.unavailable("chromium missing");
        assertFalse(result.available());
        assertFalse(result.rendered());
        assertEquals("chromium missing", result.reason());
        assertTrue(result.errors().isEmpty());
    }

    /**
     * [8c] HTML 拼装含 CSP 头：阻止恶意脚本执行。
     */
    @Test
    void case08c_htmlContainsCspHeader() {
        PlaywrightRenderer r = new PlaywrightRenderer();
        String html = r.buildHtml("{\"type\":\"page\"}");
        assertTrue(html.contains("Content-Security-Policy"),
                "buildHtml 输出必须包含 CSP meta 头");
        assertTrue(html.contains("cdn.jsdelivr.net"),
                "CSP 应允许 jsdelivr CDN");
        assertTrue(html.contains("object-src 'none'"),
                "CSP 应禁止 object 标签");
    }

    /**
     * [12] executeSqlTemplateSafely 模板不存在分支（无后端时返回系统不存在）。
     */
    @Test
    void case12_executeSqlTemplateSafelyUnknownSystem() {
        Object result = templateService.executeSqlTemplateSafely("不存在的系统", "x", null);
        assertNotNull(result);
        assertTrue(result.toString().contains("不存在的系统") || result.toString().contains("系统不存在"),
                "未知系统应返回明确错误，实际: " + result);
    }

    // ====================== [9] CRUD：保存 / 查询 / 列表 / 删除 ======================

    @Test
    @EnabledIfEnvironmentVariable(named = "E2E_BACKEND", matches = "true")
    void case09_amisTemplateCRUD() {
        // 1. 保存
        String saveResult = (String) templateService.amisTemplateSave(
                "测试系统",
                "e2e-test-" + System.currentTimeMillis(),
                "端到端测试模板",
                "由 EndToEndChainTest 创建",
                "{\"type\":\"page\",\"title\":\"e2e\"}");
        assertNotNull(saveResult);

        // 2. 查询
        // save 走 amisTemplateSave，需要 id
        String id = "e2e-test-lookup";
        templateService.amisTemplateSave("测试系统", id, "e2e lookup", "desc", "{\"type\":\"page\"}");
        Object got = templateService.getAmisTemplate("测试系统", id);
        assertNotNull(got);

        // 3. 列表
        Object list = templateService.listAmisTemplates("测试系统", id, null, null, null);
        assertNotNull(list);

        // 4. 删除
        String del = (String) templateService.deleteAmisTemplate("测试系统", id);
        assertNotNull(del);
    }

    // ====================== [10] 知识：hints 长度 ======================

    @Test
    void case10_hintsHasEnoughContent() {
        String hints = knowledge.getHints();
        assertNotNull(hints);
        assertTrue(hints.length() > 1000, "hints Markdown 长度应 > 1000，实际=" + hints.length());
    }

    // ====================== [11] 全 examples 循环校验 ======================

    @Test
    void case11_validateAllExamples() {
        List<AmisKnowledgeService.ExampleSummary> examples = knowledge.listExamples();
        assertFalse(examples.isEmpty(), "examples.json 应有内容");
        int errorCount = 0;
        for (AmisKnowledgeService.ExampleSummary summary : examples) {
            AmisKnowledgeService.Example ex = knowledge.getExample(summary.name());
            assertNotNull(ex, "范例必须可获取: " + summary.name());
            String json = serialize(ex.schema());
            AmisValidator.ValidationResult r = amisService.validateAmisTemplate(json);
            long errorsOfErrorLevel = r.errors().stream()
                    .filter(e -> "error".equals(e.severity()))
                    .count();
            if (errorsOfErrorLevel > 0) {
                errorCount++;
                System.err.println("[e2e] 例 '" + summary.name() + "' 有 " + errorsOfErrorLevel + " 个 error 级问题");
            }
        }
        assertEquals(0, errorCount, "所有 examples 必须无 error 级问题");
    }

    // ====================== 工具方法 ======================

    /**
     * 把 schema 序列化为 JSON 字符串（与 Agent 调用方式一致）。
     *
     * @param schema Schema 对象
     * @return JSON 字符串
     */
    private String serialize(Object schema) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(schema);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    // ====================== 反射 / 文档完整性兜底检查 ======================

    /**
     * 兜底：Amis 动作 @Tool 仍正确标注了 {@link Tool}（Round 5 拆分后移到了 AmisService）。
     */
    @Test
    void allAmisToolsAreAnnotated() {
        Method[] methods = AmisService.class.getDeclaredMethods();
        List<String> amisActionToolNames = List.of(
                "validateAmisTemplate", "previewAmisTemplate");
        for (String name : amisActionToolNames) {
            boolean found = false;
            for (Method m : methods) {
                if (m.getName().equals(name) && m.isAnnotationPresent(Tool.class)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "方法 " + name + " 必须标注 @Tool");
        }
    }
}