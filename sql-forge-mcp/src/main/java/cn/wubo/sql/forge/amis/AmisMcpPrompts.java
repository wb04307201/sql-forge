package cn.wubo.sql.forge.amis;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把 sql-forge-mcp 的高频工作流封装为 MCP Prompt 模板。
 * <p>
 * 与 Tools（可调用函数）/ Resources（可读取数据）不同，Prompts 是预定义的"提示模板"，
 * 用户/AI Agent 通过 slash command 触发时拿到一份结构化的多轮指导（system + user messages），
 * 适合标准化重复工作流。
 * </p>
 * <p>
 * 当前暴露 3 个 Prompt：
 * </p>
 * <ul>
 *   <li>{@code create-amis-page} — 标准化"创建 Amis 页面"迭代流程（先 validate 再 preview 再修）</li>
 *   <li>{@code diagnose-render-error} — 解读 previewAmisTemplate 错误并给出修复建议</li>
 *   <li>{@code quick-crud-template} — 根据表名一键生成 CRUD 模板骨架</li>
 * </ul>
 */
@Configuration
public class AmisMcpPrompts {

    /**
     * 暴露 3 个 Prompt 模板为 Spring Bean，让 spring-ai-mcp-server 自动注册到 MCP server。
     *
     * @return 3 个 {@link McpServerFeatures.SyncPromptSpecification} 列表
     */
    @Bean
    public List<McpServerFeatures.SyncPromptSpecification> amisPrompts() {
        return List.of(
                createAmisPagePrompt(),
                diagnoseRenderErrorPrompt(),
                quickCrudTemplatePrompt());
    }

    // ============================================================
    // Prompt 1: create-amis-page
    // ============================================================

    /**
     * 标准化"创建 Amis 页面"工作流 Prompt。
     * <p>
     * 用户调用方式（在 MCP 客户端通常为 slash command）：
     * </p>
     * <pre>
     * /create-amis-page systemName="TestSys" tableName="PRODUCTS" pageTitle="商品管理"
     * </pre>
     * <p>
     * Prompt 返回的 messages 引导 AI Agent 按以下步骤工作：
     * </p>
     * <ol>
     *   <li>读 amis://schema-hints 学习 schema 规范</li>
     *   <li>读 amis://examples/crud-page 拿 CRUD 范例</li>
     *   <li>用 getMetaDataTableInfo 拉表结构</li>
     *   <li>拼出 schema 后用 validateAmisTemplate 静态校验</li>
     *   <li>用 previewAmisTemplate 真实渲染</li>
     *   <li>检查 errors，有错就修，没有就 amisTemplateSave</li>
     * </ol>
     */
    private McpServerFeatures.SyncPromptSpecification createAmisPagePrompt() {
        McpSchema.Prompt prompt = new McpSchema.Prompt(
                "create-amis-page",
                "创建 Amis 页面",
                "按标准迭代流程创建 Amis 页面：先 schema-hints/examples，再 getMetaDataTableInfo 拉结构，" +
                        "再 validate，再 preview，最后 amisTemplateSave。",
                List.of(
                        new McpSchema.PromptArgument("systemName", "系统名称",
                                "后端系统名（必填，如 TestSys）", true),
                        new McpSchema.PromptArgument("tableName", "目标表名",
                                "CRUD 涉及的数据库表名（必填，如 PRODUCTS）", true),
                        new McpSchema.PromptArgument("pageTitle", "页面标题",
                                "Amis 页面 title（可选，默认=表名+'管理'）", false)));

        return new McpServerFeatures.SyncPromptSpecification(prompt, (exchange, request) -> {
            Map<String, Object> args = request.arguments() == null
                    ? Map.of() : request.arguments();
            String systemName = strArg(args, "systemName", "TestSys");
            String tableName = strArg(args, "tableName", "");
            String pageTitle = strArg(args, "pageTitle", tableName + "管理");

            String userMsg = """
                    你正在按 sql-forge-mcp 标准流程创建一个 Amis 页面。

                    ## 目标参数
                    - 系统名 (systemName): `%s`
                    - 目标表 (tableName): `%s`
                    - 页面标题 (pageTitle): `%s`

                    ## 必须按顺序执行的步骤

                    ### Step 1: 加载 Amis 知识（并行）
                    并行调用以下 3 个 MCP Resource：
                    - `amis://schema-hints` — 学习 Schema 规范、API 三种写法、JSON CRUD body 结构
                    - `amis://components` — 找到需要的组件（如 crud, table, input-text）
                    - `amis://examples/crud-page` — 参考标准 CRUD 页面范例

                    ### Step 2: 获取目标表结构
                    调用 MCP Tool `getMetaDataTableInfo` 拉表结构，参数：
                    - systemName: `%s`
                    - tableName: `%s`
                    - tableType: TABLE
                    重点关注：主键列名（编辑/删除用）、外键列（用作 select 选项）。

                    ### Step 3: 构造初始 schema
                    基于表结构拼出 CRUD 页面 JSON Schema：
                    - type=page, title=`%s`
                    - body.type=crud
                    - body.api 必须用 POST 方法（SQL Forge 后端 select 接口只接 POST）：
                      ```json
                      {
                        "method": "post",
                        "url": "http://localhost:8081/sql/forge/api/json/select/%s",
                        "headers": {"X-Api-Key": "test"},
                        "data": {"@column": ["col1", "col2", ...], "@where": [], "@order": ["id ASC"]}
                      }
                      ```
                    - body.columns 每个字段给 name（列名）+ label（中文标签）

                    ### Step 4: 静态校验
                    调用 `validateAmisTemplate(context)` 静态校验。
                    若 errors 非空 → 修复 schema 后重跑 Step 4。

                    ### Step 5: 真实渲染
                    调用 `previewAmisTemplate(systemName="%s", context)` 真实 Chromium 渲染。
                    - 若 rendered=false → 看 reason，可能是 Chromium 启动失败
                    - 若 errors 非空 → 常见错误及修复：
                      - `localStorage SecurityError` → 已自动 shim，不应再出现
                      - `cookie SecurityError` → 已自动 shim，不应再出现
                      - `CORS policy blocks` → 检查后端 sql.forge.cors.enabled
                      - `接口报错：{"status": 500}` → 检查 api 配置，常见是 GET/POST 用错
                      - `ReferenceError: ... is not defined` → schema 用了未注册的组件

                    ### Step 6: 保存
                    全部干净（errors=[]）后调用 `amisTemplateSave(systemName="%s", id, pageTitle, description, context)` 保存。
                    id 用驼峰英文 + 时间戳避免冲突。

                    ## 终止条件
                    - 静态校验 errors=[]
                    - previewAmisTemplate rendered=true && errors=[]
                    - amisTemplateSave 返回 true

                    任何一步失败都要回到 Step 3 重新调整 schema，不要跳过。
                    """
                    .formatted(systemName, tableName, pageTitle,
                            systemName, tableName, pageTitle, tableName, systemName, systemName);

            List<McpSchema.PromptMessage> messages = new ArrayList<>();
            messages.add(new McpSchema.PromptMessage(
                    McpSchema.Role.ASSISTANT,
                    new McpSchema.TextContent("我会按 sql-forge-mcp 标准迭代流程创建 Amis 页面，每步都会报告状态。")));
            messages.add(new McpSchema.PromptMessage(
                    McpSchema.Role.USER,
                    new McpSchema.TextContent(userMsg)));
            return new McpSchema.GetPromptResult(
                    "create-amis-page 引导：从知识加载 → 表结构 → schema 构造 → 校验 → 渲染 → 保存",
                    messages);
        });
    }

    // ============================================================
    // Prompt 2: diagnose-render-error
    // ============================================================

    /**
     * 解读 previewAmisTemplate 错误并给出修复建议。
     * <p>
     * 用户调用方式：
     * </p>
     * <pre>
     * /diagnose-render-error context="{...amis schema...}" errors="[preview errors JSON]"
     * </pre>
     */
    private McpServerFeatures.SyncPromptSpecification diagnoseRenderErrorPrompt() {
        McpSchema.Prompt prompt = new McpSchema.Prompt(
                "diagnose-render-error",
                "诊断 Amis 渲染错误",
                "解读 previewAmisTemplate 返回的 errors 数组，按错误类型给出对应的修复建议。",
                List.of(
                        new McpSchema.PromptArgument("context", "Amis schema JSON",
                                "原始 schema 字符串（必填）", true),
                        new McpSchema.PromptArgument("errors", "渲染错误 JSON",
                                "previewAmisTemplate 返回的 errors 数组的 JSON 字符串（必填）", true)));

        return new McpServerFeatures.SyncPromptSpecification(prompt, (exchange, request) -> {
            Map<String, Object> args = request.arguments() == null
                    ? Map.of() : request.arguments();
            String context = strArg(args, "context", "");
            String errors = strArg(args, "errors", "[]");

            String userMsg = """
                    你正在诊断 sql-forge-mcp previewAmisTemplate 渲染错误。

                    ## 输入
                    **Schema (context)**：
                    ```json
                    %s
                    ```

                    **Errors (预览返回的)**：
                    ```json
                    %s
                    ```

                    ## 诊断流程

                    ### 1) 错误分类
                    每个 error 有 `source` 字段：pageerror / console / network / render。
                    按 source 分组，逐条分析。

                    ### 2) 常见错误与修复
                    | 错误模式 | 根因 | 修复 |
                    |---|---|---|
                    | `SyntaxError: Unexpected token` | context 不是合法 JSON | 用 JSON.parse 验证 |
                    | `Refused to load stylesheet/script` | CSP 阻止外部 SDK | 已修复（用 https: scheme） |
                    | `localStorage SecurityError` | about:blank 无 localStorage | 已自动 shim |
                    | `cookie SecurityError` | about:blank 无 cookie | 已自动 shim |
                    | `replaceState ... URL '' cannot` | history 在 about:blank 受限 | 已自动 shim |
                    | `CORS policy blocks` from 'null' | 后端未启用 CORS | 启用 sql.forge.cors.enabled |
                    | `接口报错 500 ... sets() is null` | @set 字段缺失 | body 加 {"@set": {...}} |
                    | `接口报错 500 ... wheres() is null` | @where 缺失或结构错 | body 加 {"@where": [...]} |
                    | `接口报错 404` | URL 路径错 | 确认 POST /sql/forge/api/json/{method}/{table} |
                    | `ReferenceError: exports is not defined` | 用了 CJS 包 lib/index.min.js | 改用 sdk/sdk.js UMD bundle |
                    | `amis.embed is not a function` | 用 window.amis.embed | 改用 amisRequire('amis/embed').embed |
                    | `页面空白 + 未报错` | schema 顶层 type 不是 page | 改为 "type": "page" |
                    | `表格不显示` | @column 字段名跟 columns[].name 不一致 | 对齐字段名 |
                    | `Amis SDK 未加载` | CDN 不可达 | 检查网络 / 换 CDN |

                    ### 3) 给出修复后的 schema
                    基于诊断，修改 context 并返回完整的修复后 schema。

                    ### 4) 建议下一步
                    - 用 validateAmisTemplate 静态校验一遍
                    - 用 previewAmisTemplate 重新渲染验证 errors 已清空
                    """
                    .formatted(context, errors);

            List<McpSchema.PromptMessage> messages = new ArrayList<>();
            messages.add(new McpSchema.PromptMessage(
                    McpSchema.Role.ASSISTANT,
                    new McpSchema.TextContent("我会按错误类型分类诊断 Amis 渲染错误，给出可执行的修复方案。")));
            messages.add(new McpSchema.PromptMessage(
                    McpSchema.Role.USER,
                    new McpSchema.TextContent(userMsg)));
            return new McpSchema.GetPromptResult(
                    "diagnose-render-error 引导：分类 → 匹配模式 → 给出修复 schema",
                    messages);
        });
    }

    // ============================================================
    // Prompt 3: quick-crud-template
    // ============================================================

    /**
     * 根据表名一键生成 CRUD 模板骨架（不渲染、不保存，只产出 schema）。
     * <p>
     * 用户调用方式：
     * </p>
     * <pre>
     * /quick-crud-template systemName="TestSys" tableName="PRODUCTS" withFilter=true
     * </pre>
     * <p>
     * Prompt 引导 AI Agent 主动拉表结构 → 拼 schema → 静态校验 → 返回结果。
     * </p>
     */
    private McpServerFeatures.SyncPromptSpecification quickCrudTemplatePrompt() {
        McpSchema.Prompt prompt = new McpSchema.Prompt(
                "quick-crud-template",
                "一键生成 CRUD 模板",
                "根据表名调 getMetaDataTableInfo 拉结构，自动生成可用的 CRUD 模板 JSON Schema。",
                List.of(
                        new McpSchema.PromptArgument("systemName", "系统名称",
                                "后端系统名（必填）", true),
                        new McpSchema.PromptArgument("tableName", "表名",
                                "数据库表名（必填）", true),
                        new McpSchema.PromptArgument("withFilter", "是否带筛选",
                                "true/false（默认 true）", false)));

        return new McpServerFeatures.SyncPromptSpecification(prompt, (exchange, request) -> {
            Map<String, Object> args = request.arguments() == null
                    ? Map.of() : request.arguments();
            String systemName = strArg(args, "systemName", "TestSys");
            String tableName = strArg(args, "tableName", "");
            String withFilter = strArg(args, "withFilter", "true");

            String userMsg = """
                    你要基于 sql-forge-mcp 一键生成一张表的 CRUD 模板。

                    ## 参数
                    - systemName: `%s`
                    - tableName: `%s`
                    - withFilter: `%s`（true=生成筛选条，false=纯净表格）

                    ## 必须执行的步骤

                    ### Step 1: 拉表结构
                    调用 MCP Tool `getMetaDataTableInfo`：
                    - systemName="%s"
                    - tableName="%s"
                    - tableType="TABLE"
                    拿到 columns / primaryKeys / foreignKeys。

                    ### Step 2: 生成 schema
                    基于 columns 拼出如下骨架（伪代码）：
                    ```
                    {
                      "type": "page",
                      "title": "<表名 + 管理>",
                      "body": {
                        "type": "crud",
                        "api": {
                          "method": "post",
                          "url": "<baseUrl>/sql/forge/api/json/select/<TABLE>",
                          "headers": {"X-Api-Key": "<apiKey>"},
                          "data": {"@column": [...all columns...], "@where": [], "@order": ["<pk> ASC"]}
                        },
                        "syncLocation": false,
                        "columns": [
                          // 主键列：quickEdit=true
                          // VARCHAR/TEXT：input-text
                          // INT/DECIMAL：input-number
                          // BOOLEAN：switch
                          // 外键：暂时不展开
                        ],
                        "headerToolbar": ["bulkActions", {"type":"button","label":"新增","actionType":"dialog","dialog":{...}}],
                        "columns": [...包含 type:operation 编辑/删除按钮...]
                      }
                    }
                    ```

                    ### Step 3: 静态校验
                    调用 `validateAmisTemplate` 校验，确保生成的 schema 合法。

                    ### Step 4: 返回
                    - 完整的 JSON Schema（格式化后）
                    - 简要说明：表中有多少列、有几个必填字段、操作列有什么按钮
                    - 提示用户下一步可调用 `/create-amis-page` 走完整流程，或直接用 `/diagnose-render-error` 排查渲染问题
                    """
                    .formatted(systemName, tableName, withFilter, systemName, tableName);

            List<McpSchema.PromptMessage> messages = new ArrayList<>();
            messages.add(new McpSchema.PromptMessage(
                    McpSchema.Role.ASSISTANT,
                    new McpSchema.TextContent("我会基于表结构生成可用的 CRUD 模板，并做静态校验。")));
            messages.add(new McpSchema.PromptMessage(
                    McpSchema.Role.USER,
                    new McpSchema.TextContent(userMsg)));
            return new McpSchema.GetPromptResult(
                    "quick-crud-template 引导：拉结构 → 生成 schema → 校验 → 返回",
                    messages);
        });
    }

    // ============================================================
    // 辅助
    // ============================================================

    /**
     * 从参数 Map 安全读取字符串参数，缺失时返回 defaultValue。
     *
     * @param args         prompt 参数 Map
     * @param key          参数名
     * @param defaultValue 缺省值
     * @return 字符串值
     */
    private static String strArg(Map<String, Object> args, String key, String defaultValue) {
        Object v = args.get(key);
        if (v == null) return defaultValue;
        return String.valueOf(v);
    }

    /**
     * 保留 map 参数入口（备用，目前 strArg 已覆盖字符串场景）。
     */
    @SuppressWarnings("unused")
    private static Map<String, Object> mapArg(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            m.forEach((k, val) -> out.put(String.valueOf(k), val));
            return out;
        }
        return Map.of();
    }
}
