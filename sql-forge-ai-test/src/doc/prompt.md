# 🎯 单表 Amis 界面生成 Prompt 模板

以下是一个**可复用、结构化**的 Prompt 模板，您只需替换 `{{TABLE_INFO}}` 和 `{{API_SPEC}}` 即可生成任意单表的 Amis CRUD 界面 JSON：

---

## 📋 Prompt 模板

```markdown
# 角色定义
你是一位精通百度 Amis 低代码框架和 RESTful API 集成的前端架构师。你的任务是根据提供的「数据库表结构」和「后端 API 规范」，生成一个功能完整的单表维护界面 Amis JSON 配置。

# 任务目标
生成一个支持「列表展示 + 分页 + 搜索 + 新增 + 编辑 + 删除 + 导出」的单表 CRUD 界面，要求：
1. 完全基于提供的 API 规范构造请求
2. 自动处理字典关联字段的展示与表单渲染
3. 字段类型智能映射（见下方规则）
4. 输出纯 JSON，无需额外解释

# 输入信息

## 1️⃣ 数据库表结构（JSON）
{{TABLE_INFO}}

## 2️⃣ 后端 API 规范摘要
{{API_SPEC}}

# 🛠️ 生成规则（请严格遵守）

## 🔹 字段类型映射规则
| 表字段特征 | Amis 表单组件 | 列表列配置 |
|-----------|------------|-----------|
| type=string + max≤100 | input-text | 默认文本展示 |
| type=string + ref存在 | select（source调用字典API） | 展示字典item_name |
| type=number/int | input-number | 右对齐数字 |
| type=boolean | switch | 是/否标签 |
| pk=true | uuid（新增时）/ 隐藏（编辑时） | 列表隐藏 |
| 字段名含 time/date | input-datetime | 格式化展示 |

## 🔹 字典关联处理（ref字段）
当字段有 `ref` 属性时：
1. 列表查询：通过 `@join` 关联 `sys_dict_items` 表，使用 `item_name AS 字段别名` 展示
2. 搜索表单：生成 `select` 组件，`source` 调用字典查询API，过滤条件 `dict_code=xxx`
3. 增/改表单：同上，`name` 使用原字段编码（如 `DICT_SEX`），`valueField=item_code`

## 🔹 API 请求构造规则
- 列表分页：`POST /sql/forge/api/json/selectPage/{tableName}`
  - `@page`: `{pageIndex: "${page - 1}", pageSize: "${perPage}"}`
  - `@order`: 支持前端排序传参 `"${orderBy && orderDir ? (orderBy + ' ' + orderDir): ''}"`
- 单条查询：`select/{tableName}` + `@where` 主键 EQ
- 新增：`insert/{tableName}` + `@set` 表单数据
- 更新：`update/{tableName}` + `@set` + `@where` 主键
- 删除：`delete/{tableName}` + `@where` 主键 IN（支持批量）
- 导出：复用 `select/{tableName}` 接口，去掉分页参数

## 🔹 搜索表单生成
- `autoGenerateFilter: true` 启用自动过滤
- 文本字段：`input-text` + `LIKE` 条件
- 字典字段：`select` + `multiple` + `IN` 条件 + `split` 处理
- 所有搜索值使用 `${字段名 | default:undefined}` 空值处理

## 🔹 界面结构要求
```json
{
  "type": "page",
  "body": {
    "type": "crud",
    "id": "crud_table",  // 固定ID，用于刷新
    "api": { ... },      // 分页查询API
    "headerToolbar": [  // 新增按钮、列切换、导出
      { "type": "button", "actionType": "drawer", "drawer": { ... } },  // 新增表单
      "bulkActions",
      { "type": "columns-toggler" },
      { "type": "export-excel", ... }
    ],
    "bulkActions": [    // 批量删除
      { "actionType": "ajax", "api": { ... }, "confirmText": "..." }
    ],
    "columns": [        // 列表列 + 操作列
      { "name": "字段", "label": "...", "sortable": true, "searchable": {...} },
      {
        "type": "operation",
        "buttons": [
          { "label": "修改", "actionType": "drawer", "drawer": { ... } },  // 编辑表单
          { "label": "删除", "actionType": "ajax", "api": {...}, "confirmText": "..." }
        ]
      }
    ]
  }
}
```

## 🔹 关键细节
1. 所有 API 的 `data` 中的变量使用 Amis 模板语法：`${变量名}`
2. 分页参数转换：Amis 页码从 1 开始 → API 要求 0 开始：`"${page - 1}"`
3. 字典查询的 `adaptor` 统一格式：
   ```js
   "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name || item.ITEM_NAME\n  }))\n};"
   ```
4. 主键字段：新增时用 `uuid` 组件自动生成，编辑/列表时隐藏
5. 操作列固定右侧：`"fixed": "right"`
6. 所有危险操作（删除）必须加 `confirmText`

# 🚫 禁止事项
- 不要硬编码表名/字段名，必须从输入表结构动态生成
- 不要遗漏字典字段的关联查询条件（如 `sex.dict_code = 'sex'`）
- 不要使用未定义的 API 路径或参数
- 不要输出 Markdown 代码块标记，只返回纯 JSON

# ✅ 输出要求
直接输出完整的 Amis JSON 配置，格式合法、缩进规范，可被 `amis.embed()` 直接渲染。

---
现在，请根据上方输入的表结构和 API 规范，生成对应的单表维护界面 JSON：
```

---

## 💡 使用示例

```javascript
// 调用大模型时替换变量
const prompt = template
  .replace('{{TABLE_INFO}}', JSON.stringify(tableSchema, null, 2))
  .replace('{{API_SPEC}}', apiSpecSummary);  // 可精简API规范，只保留关键部分

// 建议的 API 规范摘要（精简版）
const apiSpecSummary = `
后端 API 基础路径: /sql/forge/api/json/{method}/{tableName}
支持方法: select | selectPage | insert | update | delete
请求格式:
- 查询: { "@column":[], "@where":[], "@join":[], "@order":[], "@page":{pageIndex,pageSize} }
- 插入: { "@set": {字段:值} }
- 更新: { "@set": {字段:新值}, "@where":[...] }
- 删除: { "@where":[...] }
字典表: sys_dict_items (item_code, dict_code, item_name)
`;
```

---

## 🔧 进阶优化建议

1. **增加字段备注映射**：在表信息的 `desc` 字段内容自动作为 `label` 或 `placeholder`
2. **必填校验**：如果表结构扩展 `nullable: false`，生成 `required: true`
3. **权限控制**：在 prompt 中增加「根据角色隐藏按钮」的规则
4. **国际化**：要求生成的 `label` 使用 `i18n: 'key'` 格式
5. **响应处理**：为 `initApi` 添加 `responseData: { "&": "${items | first}" }` 自动提取单条数据

---

## ⚠️ 常见问题规避

| 问题 | 解决方案 |
|-----|---------|
| 字典字段查询重复关联 | 在 `@where` 中固定 `dict_code='xxx'` 条件 |
| 分页页码偏移 | 明确写死 `"pageIndex": "${page - 1}"` |
| 多字典表关联别名冲突 | join 时给字典表加随机后缀：`sys_dict_items sex_${random}` |
| 表单提交字段名错误 | 确保 `@set` 中使用数据库原始字段名（如 `DICT_SEX`），而非展示别名 |

---

> 📌 **提示**：将此 prompt 保存为模板文件，配合脚本自动注入表结构 JSON，即可实现「输入表定义 → 输出 Amis 界面」的自动化流程。

如果需要我针对某个具体表结构演示生成效果，或调整 prompt 适配您的项目规范，请随时告知！🚀