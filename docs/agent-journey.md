# sql-forge-mcp Claude Code 实操 Agent Journey

本文档面向 Claude Code 用户：实际用 MCP 工具构造 Amis CRUD 页面的完整流程。

## 场景

> "用 MCP 工具给 USERS 表构造一个 CRUD 页面，渲染验证，保存到系统"

## 前置

1. **sql-forge-mcp 已配置** 在 `.mcp.json`（含 `--java-options=-Dfile.encoding=UTF-8`）
2. **后端 sql-forge-test 已启动** 在 `:8081`（或自己配置）
3. **Claude Code 已 reconnect**（`/mcp` 命令）

## 步骤

### Step 1: 健康检查
```
请 Claude Code 调用 mcp__sql-forge-mcp__mcpHealth，确认返回 status=UP。
```

期望返回：
```json
{
  "status": "UP",
  "backends": {"TestSys": {"status": "UP", "latencyMs": 23}}
}
```

### Step 2: 表结构探索
```
请 Claude Code 调用 mcp__sql-forge-mcp__describeSchema，参数 systemName="TestSys" tableNamePattern="USERS"。
```

期望返回包含列：ID / USERNAME / PASSWORD / ENABLED / CATEGORY。

### Step 3: 查阅 Amis 知识
```
请 Claude Code 调用 mcp__sql-forge-mcp__AmisMcpResources.readResource，URI="amis://examples/crud-page"。
```

期望返回完整的 CRUD 模板（含 page / body / filter / columns / headerToolbar）。

### Step 4: 构造页面
让 Claude Code 根据 Step 2 + Step 3 拼一个 Amis page schema，类似：

```json
{
  "type": "page",
  "title": "用户管理",
  "body": {
    "type": "crud",
    "api": "POST /sql/forge/api/json/select/USERS",
    "columns": [
      {"name": "ID", "label": "ID"},
      {"name": "USERNAME", "label": "用户名"}
    ]
  }
}
```

### Step 5: 校验
```
请 Claude Code 调用 mcp__sql-forge-mcp__validateAmisTemplate，把 Step 4 拼的 schema 作为 context。
```

期望：`{valid: true, errors: []}`。

### Step 6: 渲染（需要 Playwright）
```
请 Claude Code 调用 mcp__sql-forge-mcp__previewAmisTemplate，参数 systemName="TestSys" context=<Step 4 schema>。
```

期望：`{available: true, rendered: true, errors: []}`（需要 Playwright 已装）。

### Step 7: 保存
```
请 Claude Code 调用 mcp__sql-forge-mcp__amisTemplateSave，
参数 systemName="TestSys" id="user_crud_001" name="用户管理" description="由 AI 构造" context=<Step 4 schema>。
```

期望返回 `"true"`。

### Step 8: 反查
```
请 Claude Code 调用 mcp__sql-forge-mcp__getAmisTemplate，参数 systemName="TestSys" id="user_crud_001"。
```

期望返回保存的 schema 完整内容（含中文字符）。

### Step 9: 清理
```
请 Claude Code 调用 mcp__sql-forge-mcp__deleteAmisTemplate，参数 systemName="TestSys" id="user_crud_001"。
```

期望返回 `"true"`。

## 验证清单

- [ ] Step 1: `mcpHealth.status = UP`
- [ ] Step 2: `describeSchema` 包含 USERS 5 个列
- [ ] Step 3: `amis://examples/crud-page` Resource 返回完整模板
- [ ] Step 5: `validateAmisTemplate.valid = true`
- [ ] Step 6: `previewAmisTemplate.rendered = true`
- [ ] Step 7: `amisTemplateSave` 返回 `"true"`
- [ ] Step 8: `getAmisTemplate` 反查内容 = Step 7 保存内容
- [ ] Step 9: `deleteAmisTemplate` 返回 `"true"`
- [ ] 最后再 `mcpHealth` 仍 UP（旅程不破坏系统状态）

## 常见错误排查

| 错误 | 原因 | 修复 |
|------|------|------|
| `mcpHealth.status = DEGRADED` | 后端不可达 | 检查 sql-forge-test 是否启动在 8081 |
| `validateAmisTemplate.valid = false` | schema 有错误字段 | 看 errors[].path 修复 |
| `amisTemplateSave` 500 | 后端没存 Amis 表 | 看后端日志 |
| `getAmisTemplate` 中文乱码 | 缺 `-Dfile.encoding=UTF-8` | 加 .mcp.json JVM 选项 |
| 工具调用卡住不返回 | stdio 字符集死锁 | **必填** `--java-options=-Dfile.encoding=UTF-8` |

## 进阶：测试 Prompts

Claude Code 还提供 3 个 Prompts 引导整个旅程：

- `/create-amis-page <systemName> <tableName> <pageTitle>` —— 标准化 CRUD 创建
- `/diagnose-render-error <context> <errors>` —— 修复渲染错误
- `/quick-crud-template <systemName> <tableName> [withFilter]` —— 一键骨架

## 相关测试（自动化版）

JUnit 端到端测试位于 `sql-forge-mcp/src/test/java/cn/wubo/sql/forge/agent/`：

| 类 | 用例 | 验证 |
|------|------|------|
| `CrudPageJourneyTest` | 3 | 标准 CRUD / 中文 / 大表 |
| `FixLoopJourneyTest` | 2 | 错误修复循环 |
| `DestructiveGuardTest` | 2 | 危险 Tool 标记 + 错误友好化 |
| `MultiTableJourneyTest` | 1 | USERS / ORDERS / PRODUCTS |
| `ResourceToolCollabJourneyTest` | 2 | Resource + Tool 协作 |
| `PlaywrightRenderTest` | 3 | **真 Chromium 渲染验证**（基础 / 模板表达式 / 错误捕获） |

合计 13 个用例，CI 跑 < 30 秒（PlaywrightRenderTest 单测约 23s 因为 Chromium 冷启动）。

### PlaywrightRenderTest 用例说明

之前有 1 个 skipped 测试（依赖 Playwright Chromium 但 mock 环境没有装）。
Round 7 补上了 `PlaywrightRenderTest` 真正启动 Chromium 验证 Amis 渲染：

| 用例 | 验证点 |
|------|--------|
| `basicPage_renderSucceeds` | 基础 page + tpl 在 Chromium 中渲染成功，errors 为空 |
| `templateExpression_doesNotBreakRender` | 含 `{{name}}` 表达式的 schema 渲染不报错 |
| `invalidBodyType_errorsCaptured` | 错误 type 的 schema 不让浏览器崩溃，错误被 PlaywrightRenderer 捕获 |

要求本地 Chromium 已装（CI 默认 skip，用 `RUN_BROWSER_TESTS=1` 开启）。
