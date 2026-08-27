# sql-forge-mcp

Spring AI MCP server for SQL Forge — exposes the SQL Forge backend API + Amis knowledge/validation/rendering chain as AI-callable tools.

## 模块约束

**本模块不持有任何 web 入口**（servlet / RouterFunction），也**不依赖**`sql-forge-web` 模块。
所有 Amis 预览通过 `page.setContent()` 自包含 HTML 在 Java 进程内完成，
不向任何业务后端发请求加载页面，Amis SDK 由浏览器按需从 jsdelivr CDN 拉取。

## Amis 工具迭代链路

AI Agent 构造 Amis 页面 JSON Schema 的完整路径：

```
getAmisSchemaHints → listAmisComponents → getAmisComponentSpec
  → getAmisExample → （修改 schema）→ validateAmisTemplate
  → previewAmisTemplate → amisTemplateSave → getAmisTemplate / listAmisTemplates
```

### 工具清单

| 工具 | 作用 |
|---|---|
| `listAmisCategories` | 列出组件分类（layout / form / crud / feedback / chart） |
| `listAmisComponents(category?)` | 列出分类下的组件（不传返回全部） |
| `getAmisComponentSpec(type)` | 返回组件必填项、关键 props、最小示例、文档 URL |
| `listAmisExamples` | 列出精选模板范例 |
| `getAmisExample(name)` | 返回某个范例的完整 JSON Schema |
| `getAmisSchemaHints` | 返回 Markdown 速查手册 |
| `validateAmisTemplate(context)` | 静态校验：JSON 语法 / 必填 props / api 格式 / 嵌套递归 |
| `previewAmisTemplate(systemName, context)` | 真实浏览器渲染（headless Chromium） |
| `amisTemplateSave` / `getAmisTemplate` / `listAmisTemplates` / `deleteAmisTemplate` | Amis 模板 CRUD（落到业务后端） |

### 渲染降级

`previewAmisTemplate` 在 Chromium 不可用时返回 `render.available=false`，
含 `installHint`，静态校验结果不受影响。

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

- `catalog.json` — 23 个组件元数据
- `examples.json` — 10 个完整可运行范例
- `schema-hints.md` — Markdown 速查手册

修改后重启 MCP 服务即可生效，无需重新编译 Java。