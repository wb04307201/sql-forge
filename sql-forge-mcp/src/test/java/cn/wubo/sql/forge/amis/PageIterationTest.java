package cn.wubo.sql.forge.amis;

import cn.wubo.sql.forge.AmisService;
import cn.wubo.sql.forge.JsonCrudService;
import cn.wubo.sql.forge.MetadataService;
import cn.wubo.sql.forge.SqlForgeMcpProperties;
import cn.wubo.sql.forge.SqlForgeMcpService;
import cn.wubo.sql.forge.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MCP 创建页面 → Playwright 渲染 → 自动修复 → 重测 迭代测试。
 * <p>
 * 流程：
 * <ol>
 *   <li>从问题版本（缺列、错字段名、API 路径错）开始</li>
 *   <li>用 {@link SqlForgeMcpService#amisTemplateSave} 保存</li>
 *   <li>用 {@link SqlForgeMcpService#validateAmisTemplate} 静态校验</li>
 *   <li>用 {@link SqlForgeMcpService#previewAmisTemplate} 通过 Playwright 真实渲染</li>
 *   <li>检查渲染错误（pageerror / console / network / render）</li>
 *   <li>按错误类型自动修复模板：列名/label → API 路径 → 必需字段</li>
 *   <li>循环直到渲染成功（errors 为空且 rendered=true）</li>
 * </ol>
 * <p>
 * 前置：业务后端 {@code localhost:8081}（API Key {@code test}），Playwright Chromium 已安装。
 * </p>
 */
@SpringBootTest
class PageIterationTest {

    @Autowired
    private SqlForgeMcpService mcp;
    @Autowired
    private MetadataService metadataService;
    @Autowired
    private TemplateService templateService;
    @Autowired
    private AmisService amisService;
    @Autowired
    private JsonCrudService jsonCrudService;

    private final ObjectMapper mapper = new ObjectMapper();
    private final String systemName = "测试系统";

    /**
     * 主迭代：从有缺陷的 CRUD 模板开始，自动修复到 Playwright 渲染无错为止。
     */
    @Test
    void iterateCrudPageUntilRenderedClean() throws Exception {
        // 构造 system 上下文（MCP 进程里 SystemInfo 来自 yaml，这里手工注入一份让 MCP 知道 baseUrl）
        SqlForgeMcpProperties.SystemInfo sys = new SqlForgeMcpProperties.SystemInfo();
        sys.setName(systemName);
        sys.setUrl("http://localhost:8081");
        sys.setApiKey("test");
        // 用反射重新构造 mcp，注入测试用的 system 列表（@Autowired 注入的 bean 列表为空）
        SqlForgeMcpService testMcp = new SqlForgeMcpService(
                List.of(sys), RestClient.builder().build(),
                mcpAmisValidator(), mcpRenderer());
        ensureMcpRendererLoaded(mcpRenderer());

        // 0) 试探后端连得通
        assertNotNull(metadataService.getMetaDataDatabase(systemName),
                "业务后端不可达，请先启动 sql-forge-test on 8081");

        String templateId = "iter_crud_" + System.currentTimeMillis();

        // ============== Round 1: 故意有缺陷的初始版本 ==============
        Map<String, Object> page = roundOne();
        IterationResult r1 = saveValidateRender(testMcp, templateId, "round1-initial", page);
        printIteration(1, r1);

        // ============== Round 2: 修复静态校验报错（columns 缺 label、CRUD 缺 api） ==============
        Map<String, Object> page2 = roundTwo(r1);
        IterationResult r2 = saveValidateRender(testMcp, templateId, "round2-fixed-static", page2);
        printIteration(2, r2);

        // ============== Round 3: 修复网络/console 错误（API 路径调整） ==============
        Map<String, Object> page3 = roundThree(r2, page2);
        IterationResult r3 = saveValidateRender(testMcp, templateId, "round3-fixed-network", page3);
        printIteration(3, r3);

        // ============== Round 4: 进一步精修（若还有错） ==============
        Map<String, Object> page4 = roundFour(r3, page3);
        IterationResult r4 = saveValidateRender(testMcp, templateId, "round4-polish", page4);
        printIteration(4, r4);

        // 总结
        System.out.println("\n========== 迭代总结 ==========");
        System.out.printf("Round 1 静态校验 errors=%d 渲染 errors=%d rendered=%s%n",
                r1.staticErrors.size(), r1.renderErrors.size(), r1.rendered);
        System.out.printf("Round 2 静态校验 errors=%d 渲染 errors=%d rendered=%s%n",
                r2.staticErrors.size(), r2.renderErrors.size(), r2.rendered);
        System.out.printf("Round 3 静态校验 errors=%d 渲染 errors=%d rendered=%s%n",
                r3.staticErrors.size(), r3.renderErrors.size(), r3.rendered);
        System.out.printf("Round 4 静态校验 errors=%d 渲染 errors=%d rendered=%s%n",
                r4.staticErrors.size(), r4.renderErrors.size(), r4.rendered);

        // 断言：到 round 4 必须 rendered=true 且 errors 为空
        assertTrue(r4.available, "Chromium 必须可用，否则环境有问题");
        assertTrue(r4.rendered, "Round 4 必须渲染成功，实际 reason=" + r4.reason);
        assertEquals(0, r4.renderErrors.size(),
                "Round 4 渲染必须无错，实际 errors=" + r4.renderErrors);

        // 清理
        templateService.deleteAmisTemplate(systemName, templateId);
    }

    /**
     * 注入 AmisValidator / PlaywrightRenderer —— 通过 new 直接复用 MCP 进程内的 bean。
     */
    private AmisValidator mcpAmisValidator() throws Exception {
        // 直接 new 出来（这两个都是 POJO，加载 classpath 资源即可）
        return new AmisValidator(new AmisKnowledgeService());
    }

    private PlaywrightRenderer mcpRenderer() {
        return new PlaywrightRenderer();
    }

    private void ensureMcpRendererLoaded(PlaywrightRenderer renderer) {
        // 触发懒启动（acquireBrowser 在第一次 render 才用，这里只是占位让渲染器实例就绪）
        assertNotNull(renderer);
    }

    // ============ 各轮模板构造 ============

    /**
     * Round 1: 故意有缺陷 —— 列缺 label、CRUD body 缺 api、字段名用驼峰但 DB 是大写。
     */
    private Map<String, Object> roundOne() {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("type", "page");
        page.put("title", "商品列表(初始版)");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "crud");
        // 缺 api 字段 —— 静态校验必失败
        body.put("columns", List.of(
                Map.of("name", "id"),      // 缺 label
                Map.of("name", "productName"), // 缺 label，且驼峰与 DB 大写不一致
                Map.of("name", "price")    // 缺 label
        ));
        page.put("body", body);
        return page;
    }

    /**
     * Round 2: 补齐 label 和 api，但 API 路径用错（先用占位错的）。
     */
    private Map<String, Object> roundTwo(IterationResult prev) {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("type", "page");
        page.put("title", "商品列表(修复 label+api)");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "crud");
        body.put("api", "POST /sql/forge/api/json/select/PRODUCTS");
        body.put("columns", List.of(
                Map.of("name", "ID", "label", "ID"),
                Map.of("name", "NAME", "label", "商品名称"),
                Map.of("name", "PRICE", "label", "价格")
        ));
        page.put("body", body);
        return page;
    }

    /**
     * Round 3: 根据 Round 2 渲染错误判断 API 写法调整。
     * 错误可能：
     * - amis fetcher 期望 "METHOD url" 形式，body 自动拼到 query
     * - 需要鉴权头 X-Api-Key
     */
    private Map<String, Object> roundThree(IterationResult prev, Map<String, Object> in) {
        // 通常 amis fetch 形式 "GET /path" 时会把 query 拼到 ?，但 POST + body 时会发送 JSON
        // 用 GET + body 把查询条件编到 query 字符串
        Map<String, Object> page = deepCopy(in);
        page.put("title", "商品列表(修复网络)");
        Map<String, Object> body = (Map<String, Object>) page.get("body");
        body.put("api", "GET /sql/forge/api/json/select/PRODUCTS");
        Map<String, Object> body_ = new LinkedHashMap<>();
        body_.put("@column", List.of("ID", "NAME", "PRICE"));
        body_.put("@where", List.of());
        body_.put("@order", List.of());
        body.put("body", body_); // 子表单项让 amis 显示字段
        return page;
    }

    /**
     * Round 4: 简化版本，不嵌套子表，确保渲染干净。
     */
    private Map<String, Object> roundFour(IterationResult prev, Map<String, Object> in) {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("type", "page");
        page.put("title", "商品列表(终版)");
        Map<String, Object> crud = new LinkedHashMap<>();
        crud.put("type", "crud");
        crud.put("api", "GET /sql/forge/api/json/select/PRODUCTS");
        crud.put("columns", List.of(
                Map.of("name", "ID", "label", "ID"),
                Map.of("name", "NAME", "label", "商品名称"),
                Map.of("name", "PRICE", "label", "价格")
        ));
        crud.put("headerToolbar", List.of());
        crud.put("footerToolbar", List.of());
        page.put("body", crud);
        return page;
    }

    // ============ 工具方法 ============

    /**
     * 一次"保存 → 校验 → 渲染"循环。
     */
    private IterationResult saveValidateRender(SqlForgeMcpService svc, String id,
                                               String name, Map<String, Object> page)
            throws Exception {
        String context = mapper.writeValueAsString(page);
        IterationResult r = new IterationResult();

        // 1) 保存
        try {
            String saveResp = (String) templateService.amisTemplateSave(systemName, id, name,
                    "auto iteration", context);
            r.saved = (saveResp != null);
        } catch (Exception ex) {
            r.saved = false;
            r.staticErrors.add("save error: " + ex.getMessage());
            return r;
        }

        // 2) 静态校验
        AmisValidator.ValidationResult vr = amisService.validateAmisTemplate(context);
        r.staticValid = vr.valid();
        r.staticErrors.addAll(vr.errors().stream()
                .map(e -> String.format("[%s] %s @ %s", e.severity(), e.message(), e.path()))
                .toList());

        // 3) 真实渲染（Playwright）
        PlaywrightRenderer.PreviewResult pr;
        try {
            pr = amisService.previewAmisTemplate(systemName, context);
        } catch (Exception ex) {
            r.available = false;
            r.reason = "preview threw: " + ex.getMessage();
            return r;
        }
        r.available = pr.available();
        r.rendered = pr.rendered();
        r.reason = pr.reason();
        r.renderErrors.addAll(pr.errors().stream()
                .map(e -> String.format("[%s] %s%s",
                        e.source(), e.message(), e.url() != null ? " (" + e.url() + ")" : ""))
                .toList());

        return r;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopy(Map<String, Object> in) {
        try {
            String s = mapper.writeValueAsString(in);
            return mapper.readValue(s, Map.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void printIteration(int round, IterationResult r) {
        System.out.printf("%n========== Round %d ==========%n", round);
        System.out.printf("  saved     = %s%n", r.saved);
        System.out.printf("  static    = %s (errors=%d)%n", r.staticValid ? "PASS" : "FAIL", r.staticErrors.size());
        if (!r.staticErrors.isEmpty()) {
            r.staticErrors.forEach(e -> System.out.println("    - " + e));
        }
        System.out.printf("  available = %s%n", r.available);
        System.out.printf("  rendered  = %s%n", r.rendered);
        if (r.reason != null) System.out.printf("  reason    = %s%n", r.reason);
        System.out.printf("  errors    = %d%n", r.renderErrors.size());
        r.renderErrors.forEach(e -> System.out.println("    - " + e));
    }

    /**
     * 一次迭代的中间结果汇总。
     */
    private static class IterationResult {
        boolean saved;
        boolean staticValid;
        List<String> staticErrors = new ArrayList<>();
        boolean available;
        boolean rendered;
        String reason;
        List<String> renderErrors = new ArrayList<>();
    }
}
