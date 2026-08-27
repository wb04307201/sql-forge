# sql-forge-mcp

Spring AI MCP server for SQL Forge — exposes the SQL Forge backend API as AI-callable Tools, and exposes Amis page-building knowledge/templates as AI-readable Resources.

> 📖 English version of this doc: [`README.en.md`](README.en.md)

## 模块约束

**本模块不持有任何 web 入口**（servlet / RouterFunction），也**不依赖**`sql-forge-web` 模块。
所有 Amis 预览通过 `page.setContent()` 自包含 HTML 在 Java 进程内完成，
不向任何业务后端发请求加载页面，Amis SDK 由浏览器按需从 jsdelivr CDN 拉取。

## MCP 三类原语一览

本模块按 MCP 规范把能力划分到三类原语（**Tools** / **Resources** / **Prompts**）：
- **Tools** = RPC / 动作（CRUD、校验、渲染、保存）
- **Resources** = 只读参考材料（知识、模板、清单，常驻上下文首选）
- **Prompts** = 任务模板（带占位符的预制指令）

AI Agent 拿到的是这三类列表，**Resources 优先常驻，Tools 按需 invoke**。

---

## 一、Resources（5 个，被动只读）

AI 客户端在初始化阶段就可看到这些 URI，按 URI 寻址读取内容。
Schema-hints 与 components / examples 索引建议在对话开始时**预读取到上下文**，
详细 schema 由 `{type}` / `{name}` 模板按需展开。

| URI | mimeType | 说明 |
|---|---|---|
| `amis://schema-hints` | `text/markdown` | 整篇 Amis Schema 速查手册（表达式、API 写法、CRUD 套路、踩坑清单）。**最建议常驻** |
| `amis://components` | `application/json` | 组件目录轻量索引（仅 `type / name / category / description`，不含 `keyProps`） |
| `amis://examples` | `application/json` | 范例轻量索引（仅 `name / title / tags / description`） |
| `amis://components/{type}` | `application/json` | 按 type 获取组件完整规格（含 `required` / `keyProps` / `minimalExample` / `docUrl`） |
| `amis://examples/{name}` | `application/json` | 按 name 获取完整可运行的 JSON Schema |

> 资源加载由 `AmisMcpResources` 暴露（spring-ai 1.1.x 用程序化 `List<McpServerFeatures.SyncResourceSpecification>` Bean 方式，不是注解方式）。

---

## 二、Tools（29 个，按需调用动作）

### Amis 动作（2 个）
| Tool | 作用 |
|---|---|
| `validateAmisTemplate(context)` | 静态校验：JSON 语法 / 必填字段 / 组件 type / api 格式 / 嵌套递归 |
| `previewAmisTemplate(systemName, context)` | 用 headless Chromium 真实渲染（自包含 HTML）。Chromium 不可用时降级返回 `render.available=false + installHint` |

### 后端 CRUD（13 个）
| Tool | 作用 |
|---|---|
| `getSystems` / `getMetaDataDatabase` / `sqlForgeMetaDataTables` / `getMetaDataTableInfo` / `getMetaDataTree` / `listExecutorNames` | 元数据查询 |
| `jsonSelect` / `jsonSelectPage` / `jsonInsert` / `jsonUpdate` / `jsonDelete` | JSON CRUD（按 `tableName` + body） |
| `findTablesByName(systemName, keyword)` | 模糊搜索表 |
| `describeSchema(systemName, tableNamePattern?)` | 一键获取完整 schema（库 → 表 → 列/主键/外键/索引） |
| `countRows(systemName, tableName, whereJson?)` | 统计行数 |

### 模板 CRUD + SQL 执行（12 个）
| Tool | 作用 |
|---|---|
| `executeSQL(systemName, sql)` | 直接执行 SQL（默认仅 SELECT，受 `sql.forge.api.database.select-only` 控制） |
| `executeSqlTemplate(systemName, id, params?)` | 执行 SQL 模板（推荐） |
| `executeSqlTemplateSafely(systemName, id, params?)` | 同上，但会先校验模板存在 + 参数完整 + 不执行未绑定占位符 SQL |
| `saveSqlTemplate` / `getSqlTemplate` / `listSqlTemplates` / `deleteSqlTemplate` | SQL 模板 CRUD |
| `amisTemplateSave` / `getAmisTemplate` / `listAmisTemplates` / `deleteAmisTemplate` | Amis 模板 CRUD（落业务后端） |

---

## Amis Agent 构造链路（一站式）

按"先读知识 → 选组件 → 改 schema → 校验 → 渲染 → 保存"的标准路径：

```
1. resources/read  amis://schema-hints            （建议常驻）
2. resources/read  amis://components              （候选组件清单）
   └ resources/read  amis://components/{type}     （需要时按 type 展开）
3. resources/read  amis://examples                （范例索引）
   └ resources/read  amis://examples/{name}       （按 name 取完整 schema 作为模板）
4. （按 hints 第 7 节映射表）修改 schema
5. tools/call      validateAmisTemplate(json)     （必须先通过）
6. tools/call      previewAmisTemplate(systemName, json) （可视化验证）
7. tools/call      amisTemplateSave(systemName, id, name, description, json)
```

---

## 三、Prompts（3 个，任务模板）

Prompts 是预制的、带占位符的指令模板，AI 客户端可作为 `/slash-command` 触发。预置 3 个：

| Prompt | 入参 | 作用 |
|---|---|---|
| `create-amis-page` | `<systemName> <tableName> <pageTitle>` | 标准化走"探表 → 读 schema-hints → 读 component spec → 改造 → 校验 → 预览"全流程 |
| `diagnose-render-error` | `<context> <errors>` | 把 preview 失败的 errors 喂回去，让 AI 改 schema 再来一次 |
| `quick-crud-template` | `<systemName> <tableName> [withFilter]` | 一键骨架：只读模式，最少对话生成最简 CRUD |

> Prometheus 设计哲学：能让用户"一句话出 CRUD"的，是骨架 + 模板 + 诊断循环，不是 50 个 Tool 灌给 Agent。

---

## 渲染降级

`previewAmisTemplate` 在 Chromium 不可用时返回 `render.available=false`，
含 `installHint`，静态校验结果不受影响。

---

## 构建与运行

```bash
# 编译
mvn -pl sql-forge-mcp clean install -DskipTests

# 启动 MCP server
mvn -pl sql-forge-mcp spring-boot:run \
    -Dspring-boot.run.jvmArguments="-Dsql.forge.mcp.systems[0].name=测试 \
        -Dsql.forge.mcp.systems[0].url=http://localhost:8081 \
        -Dsql.forge.mcp.systems[0].apiKey=test"

# 测试
mvn -pl sql-forge-mcp test
```

## 知识资源

手工精选的资源文件位于 `src/main/resources/amis/`：

- `catalog.json` — 54 个组件元数据（按 category 分组：crud / form / input / display / others）
- `examples.json` — 17 个完整可运行范例（crud-page / form-page / dialog-form / wizard / tabs / 等）
- `schema-hints.md` — Markdown 速查手册

修改后重启 MCP 服务即可生效，无需重新编译 Java。

## 重构说明（v1.7+）

`SqlForgeMcpService` 上的 6 个 Amis 知识 Tool（`listAmisCategories` / `listAmisComponents` /
`getAmisComponentSpec` / `listAmisExamples` / `getAmisExample` / `getAmisSchemaHints`）
已迁移为 MCP Resource，原因是这些 API 本质是"只读参考数据"而不是"动作"——
把它们从 Tool 列表搬到 Resource 列表，能让 AI 客户端在初始化阶段就把
`amis://schema-hints`（最常驻、最高价值的 Markdown 速查手册）**预读到系统提示**，
而不是让模型主动 invoke 才返回。

---

## Agent 完整操作链路

完整 11 步链路示例（用 MCP 工具组做 USERS crud-page:探表 → 读知识 → 拼装 → 校验 → 保存 → 渲染 → 改造 → 再保存 → 反查 → 删除）见仓库根目录的
[`docs/agent-journey.md`](../../docs/agent-journey.md)。
