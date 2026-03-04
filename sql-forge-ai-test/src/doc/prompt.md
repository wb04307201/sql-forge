# 🎯 Amis CRUD 单表维护界面生成 Prompt 模板

以下是一个**可复用、结构化**的 Prompt 模板，您只需替换 `{{TABLE_INFO}}` 和 `{{API_SPEC}}` 即可用于任意单表：

---

## 📋 Prompt 模板

```markdown
# 角色定义
你是一位资深前端工程师，精通百度 Amis 低代码框架和 RESTful API 集成。你的任务是根据提供的「表结构信息」和「API 规范」，生成符合规范的 Amis CRUD 页面 JSON 配置。

# 任务目标
生成一个完整的单表维护界面（CRUD），支持：
✅ 分页列表展示（含字典项关联显示）
✅ 多条件搜索过滤（支持文本模糊、下拉多选、时间范围等）
✅ 新增记录（表单校验 + 字典项下拉）
✅ 编辑记录（回显 + 字典项下拉）
✅ 单条/批量删除（含二次确认）
✅ 列显示切换、导出 Excel、排序

# 输入信息

## 1️⃣ 表结构信息（JSON）
{{TABLE_INFO}}

## 2️⃣ API 规范（关键摘要）
{{API_SPEC}}

> 🔑 核心规则：
> - 请求路径：`/sql/forge/api/json/{method}/{tableName}?executorName={executorName}`
> - 请求方法：`POST`，Content-Type: `application/json`
> - method 取值：`select` | `selectPage` | `insert` | `update` | `delete`
> - 查询条件使用 `@where` 数组，格式：`{"column": "字段", "condition": "条件类型", "value": "值"}`
> - 条件类型支持：EQ, NOT_EQ, GT, LT, LIKE, IN, BETWEEN, IS_NULL 等
> - 分页参数：`"@page": {"pageIndex": 0, "pageSize": 10}`（pageIndex 从 0 开始）
> - 关联查询使用 `@join`，字典项需关联 `sys_dict_items` 并过滤 `dict_code`

# 输出要求

## 📦 输出格式
直接输出**纯 JSON**，不要包裹 Markdown 代码块，不要额外解释。

## 🧱 Amis 页面结构要求
```json
{
  "type": "page",
  "body": {
    "type": "crud",
    "id": "crud_table",
    "api": { /* 分页查询 API 配置 */ },
    "headerToolbar": [ /* 新增、导出、列切换 */ ],
    "footerToolbar": [ /* 分页控件 */ ],
    "bulkActions": [ /* 批量删除 */ ],
    "columns": [ /* 列定义 + 操作按钮 */ ]
  }
}
```

## 🔧 字段映射规则
| 表字段类型 | Amis 组件 | 搜索组件 | 备注 |
|-----------|----------|----------|------|
| string + search:true | input-text | input-text + LIKE |  maxLength 取自字段 max |
| string + ref(字典) | select | select + 多选 | source 调用 sys_dict_items |
| number | input-number | input-number | - |
| date/datetime | input-datetime | input-datetime-range | - |
| boolean | switch | - | - |
| 主键 | hidden | - | 新增时用 uuid 组件生成 |

## 🎯 关键实现细节
1. **字典项关联**：
   - 列表显示：`@join` 关联 `sys_dict_items`，查询 `item_name` 并 `AS 别名`
   - 表单下拉：`source` 调用 `select/sys_dict_items`，过滤 `dict_code='xxx'`
   - 搜索条件：字典字段搜索用 `IN` 条件，值用 `${SEX | default:undefined | split}` 处理多选

2. **API 数据绑定**：
   - 分页查询：`@page.pageIndex = "${page - 1}"`（Amis 页码从 1 开始）
   - 排序：`@order = ["${orderBy && orderDir ? (orderBy + ' ' + orderDir) : ''}"]`
   - 搜索值：使用 `${字段名 | default:undefined}` 空值处理

3. **表单交互**：
   - 新增：使用 `drawer` + `form`，提交后 `reload` 表格
   - 编辑：`initApi` 回显数据，`responseData: { "&": "${items | first}" }`
   - 删除：单条/批量均用 `@where + IN/EQ`，添加 `confirmText`

4. **主键处理**：
   - 新增时 ID 字段用 `"type": "uuid"` 自动生成
   - 更新/删除时通过 `@where + EQ` 锁定主键

# 约束条件
⚠️ 严格遵守：
1. 所有 API 请求必须使用 `POST` + JSON Body，**不得**使用 URL 参数传查询条件
2. 字典项关联必须添加 `{"column": "sex.dict_code", "condition": "EQ", "value": "sex"}` 过滤条件
3. 搜索表单字段名必须与 `@where` 中的 `column` 对应，支持 `${xxx | default:undefined}` 空值保护
4. 批量操作使用 `IN` 条件，值格式：`"${ids | split}"`
5. 不要生成后端代码，只输出 Amis JSON 配置
6. 保持 JSON 格式合法，缩进 2 空格

# 示例参考（简化版）
<details>
<summary>📌 查询 API 配置示例</summary>

```json
"api": {
  "method": "post",
  "url": "/sql/forge/api/json/selectPage/USERS",
  "data": {
    "@column": ["USERS.ID", "USERS.USERNAME", "sex.item_name as SEX"],
    "@join": [{"type": "JOIN", "joinTable": "sys_dict_items sex", "on": "USERS.DICT_SEX = sex.item_code"}],
    "@where": [
      {"column": "USERS.USERNAME", "condition": "LIKE", "value": "${USERNAME | default:undefined}"},
      {"column": "sex.dict_code", "condition": "EQ", "value": "sex"}
    ],
    "@page": {"pageIndex": "${page - 1}", "pageSize": "${perPage}"},
    "@order": ["${orderBy && orderDir ? (orderBy + ' ' + orderDir) : ''}"]
  }
}
```
</details>

# 开始生成
请根据上方提供的【表结构信息】和【API 规范】，生成完整的 Amis CRUD 页面 JSON 配置：
```

---

## 🔄 使用方式

```javascript
// 伪代码：调用大模型
const prompt = template
  .replace('{{TABLE_INFO}}', JSON.stringify(tableSchema, null, 2))
  .replace('{{API_SPEC}}', apiSpecSummary); // 可精简 API 规范，保留关键规则

const response = await callLLM(prompt);
const amisJson = JSON.parse(response); // 直接解析使用
```

---

## 💡 优化建议

### 1. API 规范精简技巧
不必传入完整 API 文档，提取关键规则即可：
```markdown
## API 规范摘要
- 路径：`/sql/forge/api/json/{method}/{table}`
- 查询条件：`@where: [{column, condition, value}]`，condition 支持 EQ/LIKE/IN/BETWEEN
- 分页：`@page: {pageIndex: 0-based, pageSize}`
- 关联：`@join: [{type, joinTable, on}]`
- 字典项：关联 sys_dict_items 时需加 `dict_code='xxx'` 过滤
```

### 2. 表信息预处理建议
在传入前可增强字段语义：
```json
{
  "fields": {
    "DICT_SEX": {
      "type": "string",
      "desc": "性别",
      "ref": {"table": "sys_dict_items", "filter": {"dict_code": "sex"}},
      "amisComponent": "select",  // ✅ 预指定组件类型
      "searchable": true
    }
  }
}
```

### 3. 后处理校验（可选）
生成后可用简单规则校验：
```javascript
// 检查必要字段
const requiredKeys = ['type', 'body', 'api', 'columns'];
requiredKeys.forEach(key => {
  if (!amisJson.body?.[key]) throw new Error(`Missing ${key}`);
});

// 检查 API 路径格式
if (!/\/sql\/forge\/api\/json\/\w+\/\w+/.test(amisJson.body.api.url)) {
  console.warn('API URL 格式可能不正确');
}
```

---

## 🎁 附加：一键调用模板（Markdown 格式）

如果您使用支持 Markdown 的 LLM 平台，可直接复制下方模板：

<details>
<summary>📋 复制即用 Prompt</summary>

````markdown
# Role
你是一位百度 Amis + 低代码 API 集成专家。

# Task
根据以下【表结构】和【API 规范】，生成单表 CRUD 的 Amis 页面 JSON 配置。

# Table Schema
```json
{{TABLE_INFO}}
```

# API Specification
```markdown
- Base URL: `/sql/forge/api/json/{method}/{tableName}`
- Method: POST, Content-Type: application/json
- Query: `@where: [{column, condition(EQ|LIKE|IN|...), value}]`
- Page: `@page: {pageIndex: 0-based, pageSize}`
- Join: `@join: [{type, joinTable, on}]`
- Dict: 关联 sys_dict_items 时必须加 `dict_code='xxx'` 过滤
```

# Output Rules
1. 直接输出纯 JSON，无 Markdown 包裹
2. 支持：分页列表、搜索、新增、编辑、删除、批量操作、字典关联
3. 字段映射：string→input-text, dict→select, pk→uuid/hidden
4. 搜索值用 `${field | default:undefined}`，分页用 `${page-1}`
5. 批量操作用 `IN` + `${ids | split}`

# Start
请生成 Amis CRUD JSON：
````
</details>

---

> ✨ **提示**：将此模板保存为 `amis-crud-prompt.md`，每次只需替换 `{{TABLE_INFO}}` 即可复用，大幅提升低代码页面生成效率！

如需我帮您针对具体表结构生成一次示例，或调整 Prompt 适配特定大模型（如 Qwen、Claude、GPT），请随时告知~ 🚀