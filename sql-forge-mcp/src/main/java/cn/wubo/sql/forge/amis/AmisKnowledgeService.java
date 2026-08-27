package cn.wubo.sql.forge.amis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Amis 知识服务：启动时一次性加载 classpath:amis/ 下的 catalog / examples / hints
 * 到内存，供 {@link cn.wubo.sql.forge.SqlForgeMcpService} 中的 {@code @Tool} 方法查询。
 * <p>
 * 本服务只读资源文件，不发起任何外部网络请求，也不依赖浏览器，
 * 所有 MCP 进程的提示词构造都走这条路径。
 * </p>
 */
public class AmisKnowledgeService {

    private final List<ComponentSpec> catalog;
    private final List<Example> examples;
    private final String hints;

    /**
     * 构造时一次性加载所有资源文件。
     *
     * @throws IOException 资源文件缺失或解析失败
     */
    public AmisKnowledgeService() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = new ClassPathResource("amis/catalog.json").getInputStream()) {
            this.catalog = mapper.readValue(in, new TypeReference<>() {});
        }
        try (InputStream in = new ClassPathResource("amis/examples.json").getInputStream()) {
            this.examples = mapper.readValue(in, new TypeReference<>() {});
        }
        try (InputStream in = new ClassPathResource("amis/schema-hints.md").getInputStream()) {
            this.hints = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 列出所有组件分类（去重、按字母排序）。
     *
     * @return 分类名称集合
     */
    public List<String> listCategories() {
        return catalog.stream()
                .map(ComponentSpec::category)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new))
                .stream()
                .toList();
    }

    /**
     * 列出某个分类下的组件概要；分类为空时返回全部。
     *
     * @param category 分类名（layout / form / crud / feedback / chart），传 null/空 表示全部
     * @return 组件概要列表（含 type / name / category / description）
     */
    public List<ComponentSummary> listComponents(String category) {
        return catalog.stream()
                .filter(c -> category == null || category.isBlank() || category.equalsIgnoreCase(c.category()))
                .map(c -> new ComponentSummary(c.type(), c.name(), c.category(), c.description()))
                .toList();
    }

    /**
     * 按 type 获取组件的完整规格：必填字段、关键 props、最小示例、文档链接。
     *
     * @param type 组件 type（如 crud、input-text）
     * @return 组件规格；找不到时返回 null
     */
    public ComponentSpec getComponentSpec(String type) {
        return catalog.stream()
                .filter(c -> c.type().equalsIgnoreCase(type))
                .findFirst()
                .orElse(null);
    }

    /**
     * 列出所有范例的概要。
     *
     * @return 范例概要列表（含 name / title / tags / description）
     */
    public List<ExampleSummary> listExamples() {
        return examples.stream()
                .map(e -> new ExampleSummary(e.name(), e.title(), e.tags(), e.description()))
                .toList();
    }

    /**
     * 按 name 获取范例的完整 schema（含可运行的 JSON Schema）。
     *
     * @param name 范例名（如 crud-page、dialog-form）
     * @return 范例；找不到时返回 null
     */
    public Example getExample(String name) {
        return examples.stream()
                .filter(e -> e.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取速查手册的 Markdown 内容。
     *
     * @return Markdown 字符串
     */
    public String getHints() {
        return hints;
    }

    /**
     * 以分类为键返回组件规格映射，便于 Agent 一次性拿到全量知识。
     *
     * @return 分类 → 组件列表
     */
    public Map<String, List<ComponentSummary>> groupedByCategory() {
        Map<String, List<ComponentSummary>> grouped = new java.util.TreeMap<>();
        for (ComponentSpec spec : catalog) {
            grouped.computeIfAbsent(spec.category(), k -> new java.util.ArrayList<>())
                    .add(new ComponentSummary(spec.type(), spec.name(), spec.category(), spec.description()));
        }
        return grouped;
    }

    // ============ DTO ============

    /**
     * 组件完整规格（catalog.json 中的元素）。
     *
     * @param type            组件 type
     * @param name            组件显示名
     * @param category        分类
     * @param description     描述
     * @param required        必填字段列表
     * @param keyProps        关键 props 字典
     * @param minimalExample  最小可运行示例
     * @param docUrl          官方文档 URL
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ComponentSpec(
            String type,
            String name,
            String category,
            String description,
            List<String> required,
            Map<String, String> keyProps,
            Map<String, Object> minimalExample,
            String docUrl) {
    }

    /**
     * 组件概要（用于 list 接口，省略 required / keyProps 等大字段）。
     *
     * @param type        组件 type
     * @param name        组件显示名
     * @param category    分类
     * @param description 描述
     */
    public record ComponentSummary(String type, String name, String category, String description) {
    }

    /**
     * 范例概要。
     *
     * @param name        范例名
     * @param title       范例标题
     * @param tags        标签
     * @param description 描述
     */
    public record ExampleSummary(String name, String title, List<String> tags, String description) {
    }

    /**
     * 范例完整数据（examples.json 中的元素）。
     *
     * @param name        范例名
     * @param title       标题
     * @param tags        标签
     * @param description 描述
     * @param schema      可运行的 JSON Schema（嵌套树）
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Example(String name, String title, List<String> tags, String description, Map<String, Object> schema) {
    }
}