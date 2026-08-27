package cn.wubo.sql.forge.amis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把 {@link AmisKnowledgeService} 中的静态知识 / 模板以 MCP Resource 暴露给 AI Agent。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>用 spring-ai 1.1.x 程序化注册（{@link McpServerFeatures.SyncResourceSpecification}
 *       / {@link McpServerFeatures.SyncResourceTemplateSpecification}），不依赖 {@code @McpResource} 注解
 *       （该注解在 1.1.x 中由 spring-ai-mcp-annotations jar 的 Provider 类引用，但实际注解类位于 2.0+）</li>
 *   <li>URI 统一以 {@code amis://} 为前缀，分两类：静态 Resource 三个 + 模板 Resource Template 两个</li>
 *   <li>索引类用轻量 URI（仅含 type/name/category，不含 keyProps/minimalExample）</li>
 *   <li>详情类用 Resource Template（按需展开）</li>
 * </ul>
 * <p>
 * 暴露的 URI 清单：
 * <ul>
 *   <li>{@code amis://schema-hints} — 整篇 Markdown 速查手册（text/markdown）</li>
 *   <li>{@code amis://components} — 组件索引（轻量：仅 type/name/category/description）</li>
 *   <li>{@code amis://examples} — 范例索引（轻量：仅 name/title/tags/description）</li>
 *   <li>{@code amis://components/{type}} — 单个组件完整规格（含 required/keyProps/minimalExample/docUrl）</li>
 *   <li>{@code amis://examples/{name}} — 单个范例完整 schema</li>
 * </ul>
 */
@Configuration
public class AmisMcpResources {

    /** URI 前缀。 */
    public static final String URI_PREFIX = "amis://";

    private static final String SCHEMA_HINTS_URI = URI_PREFIX + "schema-hints";
    private static final String COMPONENTS_INDEX_URI = URI_PREFIX + "components";
    private static final String EXAMPLES_INDEX_URI = URI_PREFIX + "examples";
    private static final String COMPONENT_TEMPLATE_URI = URI_PREFIX + "components/{type}";
    private static final String EXAMPLE_TEMPLATE_URI = URI_PREFIX + "examples/{name}";

    /** 用正则从 URI 中提取单段路径变量，比 URI 模板 API 更直接。 */
    private static final Pattern COMPONENT_URI_PATTERN =
            Pattern.compile("^" + Pattern.quote(URI_PREFIX) + "components/([^/]+)$");
    private static final Pattern EXAMPLE_URI_PATTERN =
            Pattern.compile("^" + Pattern.quote(URI_PREFIX) + "examples/([^/]+)$");

    private final AmisKnowledgeService knowledge;
    private final ObjectMapper mapper = new ObjectMapper();

    public AmisMcpResources(AmisKnowledgeService amisKnowledgeService) {
        this.knowledge = amisKnowledgeService;
    }

    // ====================== 静态 Resource（无路径变量） ======================

    /**
     * 静态 MCP Resources：schema-hints / components / examples。
     *
     * @return 三个 {@link McpServerFeatures.SyncResourceSpecification} 列表
     */
    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> amisStaticResources() {
        List<McpServerFeatures.SyncResourceSpecification> specs = new ArrayList<>();

        // 1. amis://schema-hints — 整篇 Markdown
        McpSchema.Resource schemaHints = new McpSchema.Resource(
                SCHEMA_HINTS_URI,
                "amis-schema-hints",
                "Amis Schema 速查手册",
                "整篇 Markdown：表达式语法、API 三种写法、CRUD 套路、表单三种用法、Dialog/Drawer 踩坑、SQL Forge 后端 CRUD 约定、踩坑清单",
                "text/markdown",
                null,
                null,
                new LinkedHashMap<>()
        );
        specs.add(new McpServerFeatures.SyncResourceSpecification(schemaHints, (exchange, request) -> {
            String uri = request.uri();
            if (!SCHEMA_HINTS_URI.equals(uri)) {
                throw new IllegalArgumentException("URI 不匹配 schema-hints: " + uri);
            }
            String text = knowledge.getHints();
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(uri, "text/markdown", text)));
        }));

        // 2. amis://components — 索引（轻量）
        McpSchema.Resource componentsIndex = new McpSchema.Resource(
                COMPONENTS_INDEX_URI,
                "amis-components-index",
                "Amis 组件目录索引",
                "54 个 Amis 组件的轻量索引（仅 type/name/category/description，不含 required/keyProps/minimalExample）。需要完整规格请按 type 读 amis://components/{type}",
                "application/json",
                null,
                null,
                new LinkedHashMap<>()
        );
        specs.add(new McpServerFeatures.SyncResourceSpecification(componentsIndex, (exchange, request) -> {
            String uri = request.uri();
            if (!COMPONENTS_INDEX_URI.equals(uri)) {
                throw new IllegalArgumentException("URI 不匹配 components: " + uri);
            }
            String json = writeJson(knowledge.listComponents(null));
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(uri, "application/json", json)));
        }));

        // 3. amis://examples — 索引（轻量）
        McpSchema.Resource examplesIndex = new McpSchema.Resource(
                EXAMPLES_INDEX_URI,
                "amis-examples-index",
                "Amis 范例索引",
                "17 个 Amis 范例的轻量索引（仅 name/title/tags/description）。需要完整 schema 请按 name 读 amis://examples/{name}",
                "application/json",
                null,
                null,
                new LinkedHashMap<>()
        );
        specs.add(new McpServerFeatures.SyncResourceSpecification(examplesIndex, (exchange, request) -> {
            String uri = request.uri();
            if (!EXAMPLES_INDEX_URI.equals(uri)) {
                throw new IllegalArgumentException("URI 不匹配 examples: " + uri);
            }
            String json = writeJson(knowledge.listExamples());
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(uri, "application/json", json)));
        }));

        return specs;
    }

    // ====================== 模板 Resource（带路径变量） ======================

    /**
     * 资源模板：amis://components/{type}、amis://examples/{name}。
     * <p>
     * 直接从 request.uri() 中解析路径变量（不用 MCP 框架的模板参数机制——1.1.x 中的 ResourceTemplate 主要用于 list 显示，
     * 由 handler 自己解析 uri 字符串）。
     * </p>
     *
     * @return 两个 {@link McpServerFeatures.SyncResourceTemplateSpecification} 列表
     */
    @Bean
    public List<McpServerFeatures.SyncResourceTemplateSpecification> amisResourceTemplates() {
        List<McpServerFeatures.SyncResourceTemplateSpecification> specs = new ArrayList<>();

        // 4. amis://components/{type} — 单个组件完整规格
        McpSchema.ResourceTemplate componentTemplate = new McpSchema.ResourceTemplate(
                COMPONENT_TEMPLATE_URI,
                "amis-component-spec",
                "Amis 组件完整规格",
                "按 type 获取完整规格（required / keyProps / minimalExample / docUrl）。type 列表见 amis://components",
                "application/json",
                null,
                new LinkedHashMap<>()
        );
        specs.add(new McpServerFeatures.SyncResourceTemplateSpecification(componentTemplate, (exchange, request) -> {
            String uri = request.uri();
            String type = extractPathVar(uri, COMPONENT_URI_PATTERN, "type");
            AmisKnowledgeService.ComponentSpec spec = knowledge.getComponentSpec(type);
            if (spec == null) {
                // 友好提示：找不到时让客户端能拿到候选清单
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "component not found: " + type);
                error.put("hint", "请先读 amis://components 获取合法 type 列表");
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("match", false);
                return new McpSchema.ReadResourceResult(
                        List.of(new McpSchema.TextResourceContents(uri, "application/json", writeJson(error))),
                        meta);
            }
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(uri, "application/json", writeJson(spec))));
        }));

        // 5. amis://examples/{name} — 单个范例完整 schema
        McpSchema.ResourceTemplate exampleTemplate = new McpSchema.ResourceTemplate(
                EXAMPLE_TEMPLATE_URI,
                "amis-example-schema",
                "Amis 范例完整 schema",
                "按 name 获取可运行的完整 JSON Schema。name 列表见 amis://examples",
                "application/json",
                null,
                new LinkedHashMap<>()
        );
        specs.add(new McpServerFeatures.SyncResourceTemplateSpecification(exampleTemplate, (exchange, request) -> {
            String uri = request.uri();
            String name = extractPathVar(uri, EXAMPLE_URI_PATTERN, "name");
            AmisKnowledgeService.Example example = knowledge.getExample(name);
            if (example == null) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "example not found: " + name);
                error.put("hint", "请先读 amis://examples 获取合法 name 列表");
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("match", false);
                return new McpSchema.ReadResourceResult(
                        List.of(new McpSchema.TextResourceContents(uri, "application/json", writeJson(error))),
                        meta);
            }
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(uri, "application/json", writeJson(example))));
        }));

        return specs;
    }

    /**
     * 从请求 URI 中提取路径变量。
     *
     * @param uri      客户端传入的完整 URI（如 {@code amis://examples/crud-page}）
     * @param pattern  匹配该 URI 模式的正则
     * @param varName  变量名（用于错误信息；实际只需要 match.group(1)）
     * @return 路径变量值
     * @throws IllegalArgumentException URI 不符合预期格式
     */
    private static String extractPathVar(String uri, Pattern pattern, String varName) {
        Matcher m = pattern.matcher(uri);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "URI " + uri + " 不符合预期模式（缺少{" + varName + "}?）");
        }
        return m.group(1);
    }

    /**
     * 把对象序列化为 JSON 字符串；失败时回退为 toString，避免资源读取彻底中断。
     *
     * @param obj 要序列化的对象
     * @return JSON 字符串
     */
    private String writeJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            // 序列化失败时退回 toString，保证 Agent 至少能看到对象结构
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("serializeError", e.getOriginalMessage());
            fallback.put("toString", String.valueOf(obj));
            try {
                return mapper.writeValueAsString(fallback);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("无法序列化 Resource payload", ex);
            }
        }
    }
}
