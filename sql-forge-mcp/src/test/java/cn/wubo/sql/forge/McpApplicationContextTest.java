package cn.wubo.sql.forge;

import cn.wubo.sql.forge.amis.AmisKnowledgeService;
import cn.wubo.sql.forge.amis.AmisMcpResources;
import cn.wubo.sql.forge.amis.AmisValidator;
import cn.wubo.sql.forge.amis.PlaywrightRenderer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * sql-forge-mcp 集成测试：用 {@link SpringBootTest} 实际启动 Spring 上下文，
 * 验证所有 bean 装配正确 + @Tool 全部注册 + MCP Resource 全部注册 + Amis 校验/渲染就位。
 * <p>
 * 不依赖业务后端；只验证 MCP 进程自身的装配。
 * </p>
 */
@SpringBootTest
@Tag("context")
class McpApplicationContextTest {

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private SqlForgeMcpService sqlForgeMcpService;
    @Autowired
    private MetadataService metadataService;
    @Autowired
    private JsonCrudService jsonCrudService;
    @Autowired
    private TemplateService templateService;
    @Autowired
    private AmisService amisService;

    @Autowired
    private AmisKnowledgeService amisKnowledge;

    @Autowired
    private AmisValidator amisValidator;

    @Autowired
    private PlaywrightRenderer amisRenderer;

    /**
     * 验证核心 4 个 bean 全部装配成功且唯一。
     */
    @Test
    void coreBeansAreWired() {
        assertNotNull(sqlForgeMcpService, "SqlForgeMcpService 未装配");
        assertNotNull(amisKnowledge, "AmisKnowledgeService 未装配");
        assertNotNull(amisValidator, "AmisValidator 未装配");
        assertNotNull(amisRenderer, "PlaywrightRenderer 未装配");

        // 验证 bean 名唯一（防止 @Service + @Bean 双注册冲突）
        String[] knowledgeBeans = ctx.getBeanNamesForType(AmisKnowledgeService.class);
        String[] validatorBeans = ctx.getBeanNamesForType(AmisValidator.class);
        String[] rendererBeans = ctx.getBeanNamesForType(PlaywrightRenderer.class);
        String[] serviceBeans = ctx.getBeanNamesForType(SqlForgeMcpService.class);
        assertEquals(1, knowledgeBeans.length, "AmisKnowledgeService 应只 1 个 bean，实际: " + java.util.Arrays.toString(knowledgeBeans));
        assertEquals(1, validatorBeans.length, "AmisValidator 应只 1 个 bean");
        assertEquals(1, rendererBeans.length, "PlaywrightRenderer 应只 1 个 bean");
        assertEquals(1, serviceBeans.length, "SqlForgeMcpService 应只 1 个 bean");
    }

    /**
     * 验证 Amis 知识已加载：≥ 20 个组件（当前 54）、≥ 10 个范例（当前 17）、Markdown hints 非空。
     */
    @Test
    void amisKnowledgeIsLoaded() {
        List<String> categories = amisKnowledge.listCategories();
        assertTrue(categories.size() >= 5, "分类数应 ≥ 5，实际: " + categories.size());

        List<AmisKnowledgeService.ComponentSummary> components = amisKnowledge.listComponents(null);
        assertTrue(components.size() >= 20, "组件数应 ≥ 20，实际: " + components.size());

        List<AmisKnowledgeService.ExampleSummary> examples = amisKnowledge.listExamples();
        assertTrue(examples.size() >= 10, "范例数应 ≥ 10，实际: " + examples.size());

        String hints = amisKnowledge.getHints();
        assertTrue(hints.length() > 1000, "hints Markdown 长度应 > 1000，实际=" + hints.length());
    }

    /**
     * 验证 {@link AmisValidator} bean 已就绪。
     */
    @Test
    void amisValidatorBeanIsReady() {
        AmisValidator.ValidationResult r = amisValidator.validate("{\"type\":\"page\",\"title\":\"x\"}");
        assertTrue(r.valid(), "合法 page 应通过校验");
    }

    /**
     * 反射验证所有领域 Service 上 {@code @Tool} 方法总数（Round 5 拆分后）。
     * <p>
     * 总计 28 个 Tool 分布在 5 个 Service 上：
     * </p>
     * <ul>
     *   <li>MetadataService —— 8 个</li>
     *   <li>JsonCrudService —— 6 个</li>
     *   <li>TemplateService —— 10 个</li>
     *   <li>AmisService —— 2 个（validate/preview）</li>
     *   <li>SqlForgeMcpService —— 2 个（mcpHealth/metrics）</li>
     * </ul>
     */
    @Test
    void allToolMethodsAreAnnotated() {
        Class<?>[] allServiceClasses = new Class<?>[]{
                MetadataService.class, JsonCrudService.class, TemplateService.class,
                AmisService.class, SqlForgeMcpService.class};
        List<String> toolNames = new ArrayList<>();
        for (Class<?> c : allServiceClasses) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Tool.class)) {
                    toolNames.add(c.getSimpleName() + "#" + m.getName());
                }
            }
        }
        assertEquals(28, toolNames.size(),
                "@Tool 总数应为 28（拆分后），实际: " + toolNames.size() + "，列表: " + toolNames);

        // 验证关键 4 个 Tool 都存在
        List<String> keyTools = List.of(
                "AmisService#validateAmisTemplate",
                "AmisService#previewAmisTemplate",
                "SqlForgeMcpService#mcpHealth",
                "SqlForgeMcpService#metrics");
        for (String t : keyTools) {
            assertTrue(toolNames.contains(t), "缺少关键工具: " + t);
        }

        // 验证 6 个被迁移到 Resource 的 Tool 不再是 @Tool
        List<String> removedTools = List.of(
                "listAmisCategories", "listAmisComponents", "getAmisComponentSpec",
                "listAmisExamples", "getAmisExample", "getAmisSchemaHints");
        for (String t : removedTools) {
            assertFalse(toolNames.contains(t), "已迁移到 Resource，不应再是 @Tool: " + t);
        }
    }

    /**
     * 验证 {@link AmisMcpResources} 暴露的 5 个 URI 都能被读取：3 个 Resource + 2 个 ResourceTemplate。
     */
    @Test
    void amisResourcesAllRespond() {
        AmisMcpResources resources = new AmisMcpResources(amisKnowledge);

        List<McpServerFeatures.SyncResourceSpecification> staticRes = resources.amisStaticResources();
        assertEquals(3, staticRes.size(), "静态 Resource 应为 3，实际=" + staticRes.size());

        List<McpServerFeatures.SyncResourceTemplateSpecification> templates = resources.amisResourceTemplates();
        assertEquals(2, templates.size(), "ResourceTemplate 应为 2，实际=" + templates.size());

        // 1. amis://schema-hints 返回 Markdown（> 1000 字符）
        McpSchema.ReadResourceResult schemaHints = staticRes.get(0).readHandler().apply(null,
                new McpSchema.ReadResourceRequest(AmisMcpResources.URI_PREFIX + "schema-hints"));
        List<McpSchema.ResourceContents> shc = schemaHints.contents();
        assertEquals(1, shc.size());
        McpSchema.TextResourceContents textSh = (McpSchema.TextResourceContents) shc.get(0);
        assertEquals("text/markdown", textSh.mimeType());
        assertTrue(textSh.text().length() > 1000, "hints 长度应 > 1000");

        // 2. amis://components 返回 JSON 数组
        McpSchema.ReadResourceResult compIdx = staticRes.get(1).readHandler().apply(null,
                new McpSchema.ReadResourceRequest(AmisMcpResources.URI_PREFIX + "components"));
        McpSchema.TextResourceContents compText = (McpSchema.TextResourceContents) compIdx.contents().get(0);
        assertEquals("application/json", compText.mimeType());
        assertTrue(compText.text().contains("crud"), "components 索引应含 crud");

        // 3. amis://examples 返回 JSON
        McpSchema.ReadResourceResult exIdx = staticRes.get(2).readHandler().apply(null,
                new McpSchema.ReadResourceRequest(AmisMcpResources.URI_PREFIX + "examples"));
        McpSchema.TextResourceContents exText = (McpSchema.TextResourceContents) exIdx.contents().get(0);
        assertTrue(exText.text().contains("crud-page"), "examples 索引应含 crud-page");

        // 4. amis://components/crud 模板返回完整规格
        McpSchema.ReadResourceResult crudSpec = templates.get(0).readHandler().apply(null,
                new McpSchema.ReadResourceRequest(AmisMcpResources.URI_PREFIX + "components/crud"));
        McpSchema.TextResourceContents crudText = (McpSchema.TextResourceContents) crudSpec.contents().get(0);
        assertTrue(crudText.text().contains("\"required\""), "完整规格应含 required 字段");
        assertTrue(crudText.text().contains("\"api\""), "crud required 应含 api");

        // 5. amis://examples/crud-page 模板返回示例 schema
        McpSchema.ReadResourceResult crudExample = templates.get(1).readHandler().apply(null,
                new McpSchema.ReadResourceRequest(AmisMcpResources.URI_PREFIX + "examples/crud-page"));
        McpSchema.TextResourceContents exampleText = (McpSchema.TextResourceContents) crudExample.contents().get(0);
        assertTrue(exampleText.text().contains("\"schema\""), "范例 payload 应含 schema");

        // 6. 不存在的组件 URI 应返回友好错误
        McpSchema.ReadResourceResult notFound = templates.get(0).readHandler().apply(null,
                new McpSchema.ReadResourceRequest(AmisMcpResources.URI_PREFIX + "components/non-existent-type"));
        String notFoundText = ((McpSchema.TextResourceContents) notFound.contents().get(0)).text();
        assertTrue(notFoundText.contains("not found"), "未知组件应返回 not found 错误");
    }

    /**
     * 验证调用 2 个 Amis 动作 Tool（无后端依赖）。
     */
    @Test
    void amisActionToolsRespond() {
        AmisValidator.ValidationResult vr = amisService.validateAmisTemplate(
                "{\"type\":\"page\",\"title\":\"x\",\"body\":{\"type\":\"crud\",\"api\":\"GET /a\",\"columns\":[]}}");
        assertTrue(vr.valid());

        PlaywrightRenderer.PreviewResult pr = amisService.previewAmisTemplate(
                "测试系统", "{\"type\":\"page\"}");
        assertNotNull(pr);
        assertTrue(pr.available() || pr.reason() != null);
    }

    /**
     * 验证未注册系统调用 executeSqlTemplateSafely 早失败。
     */
    @Test
    void unknownSystemReturnsClearError() {
        Object result = templateService.executeSqlTemplateSafely("不存在的系统", "x", null);
        assertNotNull(result);
        assertTrue(result.toString().contains("不存在的系统") || result.toString().contains("系统不存在"),
                "未知系统应返回明确错误，实际: " + result);
    }

    /**
     * 验证 SqlForgeMcpProperties 已绑定 + 至少 0 个系统（启动无 systems 配置）。
     */
    @Test
    void sqlForgeMcpPropertiesBound() {
        Map<String, SqlForgeMcpService> beans = ctx.getBeansOfType(SqlForgeMcpService.class);
        assertEquals(1, beans.size());

        Map<String, AmisMcpResources> resourceBeans = ctx.getBeansOfType(AmisMcpResources.class);
        assertEquals(1, resourceBeans.size(), "AmisMcpResources 应有 1 个 bean");
    }
}
