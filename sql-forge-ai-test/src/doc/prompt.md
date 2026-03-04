# 🎯 单表 Amis 界面生成 Prompt 模板

以下是一个**可复用、结构化**的 Prompt 模板，您只需替换 `{{TABLE_INFO}}` 和 `{{API_SPEC}}` 即可生成任意单表的 Amis CRUD 界面 JSON。

---

## 📋 Prompt 模板

```markdown
# 角色设定
你是一名资深前端工程师，精通百度 Amis 低代码框架和 RESTful API 集成。你的任务是根据提供的「表结构信息」和「API 规范」，生成一个功能完整的单表维护 Amis 界面 JSON。

# 任务目标
生成一个支持【分页查询 + 新增 + 编辑 + 删除 + 批量删除 + 导出 + 字典关联 + 条件筛选】的 Amis CRUD 页面 JSON。

# 输入信息

## 1️⃣ 表结构信息
{{TABLE_INFO}}

## 2️⃣ API 规范
{{API_SPEC}}

# 输出要求

## ✅ 必须生成的功能模块
1. **CRUD 主体**：使用 `type: "crud"`，配置 `api` 对接 `selectPage` 接口
2. **查询表单**：`autoGenerateFilter: true`，字符串字段用 LIKE，枚举字段用 IN+select
3. **列配置**：
   - 主键列 `hidden: true`
   - 字符串字段配置 `searchable` + `maxLength`
   - 关联字典字段使用 `LEFT_OUTER_JOIN` 显示 `item_name`
4. **新增功能**：headerToolbar 按钮 + drawer 表单，对接 `insert` 接口，提交后 `reload` 表格
5. **编辑功能**：操作列按钮 + drawer 表单，`initApi` 预填数据，对接 `update` 接口
6. **删除功能**：单行删除 + 批量删除，对接 `delete` 接口，带 `confirmText` 确认
7. **导出功能**：export-excel 组件，对接 `select` 接口（非分页）
8. **分页配置**：`@page` 参数转换 `pageIndex: ${page - 1}`（Amis 页码从 1 开始）

## 🔧 技术约束
1. **字段映射规则**：
   - 表字段大写 → Amis 中保持大写或用 `as` 别名
   - 字典关联：`DICT_XXX` 字段 → JOIN `sys_dict_items` → 显示 `item_name`
   
2. **API 参数规范**：
   - 查询条件：`@where` 数组，LIKE 条件值用 `${XXX | default:undefined}`
   - 多选筛选：IN 条件值用 `${XXX | default:undefined | split}`
   - 排序：`@order` 用 `${default(orderBy && orderDir ? (orderBy + ' ' + orderDir):'',undefined)}`
   - 分页：`pageIndex: "${page - 1}", pageSize: "${perPage}"`

3. **字典下拉源**：
   ```json
   "source": {
     "method": "post",
     "url": "/sql/forge/api/json/select/sys_dict_items",
     "data": {
       "@column": ["item_code", "item_name"],
       "@where": [{"column": "dict_code", "condition": "EQ", "value": "字典编码"}]
     },
     "adaptor": "return { options: payload.map(item => ({ value: item.item_code, label: item.item_name })) };"
   }
   ```

4. **主键处理**：
   - 新增：使用 `type: "uuid"` 组件或后端生成
   - 编辑/删除：`@where` 条件必须包含主键 EQ

5. **安全规范**：
   - 所有用户输入使用 `${xxx | default:undefined}` 防空值
   - 删除操作必须配置 `confirmText`

## 📦 输出格式
直接返回**纯 JSON**，不要包裹 markdown 代码块，不要添加解释文字。JSON 必须：
- 语法合法，可通过 JSON.parse()
- 缩进 2 空格，便于阅读
- 关键组件添加 `"id"` 属性（如 `crud_table`, `insert-xxx`, `update-xxx`）

# 示例参考（结构示意，非完整内容）
```json
{
  "type": "page",
  "body": {
    "type": "crud",
    "id": "crud_table",
    "api": { /* 对接 selectPage */ },
    "headerToolbar": [ /* 新增/导出/列切换 */ ],
    "bulkActions": [ /* 批量删除 */ ],
    "columns": [ /* 字段列 + 操作列 */ ]
  }
}
```

# 开始生成
请根据以上要求，为表 `{{TABLE_NAME}}` 生成 Amis 界面 JSON：
```

---

## 🔄 使用示例

### 步骤 1：准备变量

```javascript
const TABLE_INFO = JSON.stringify([{
  "table": "PRODUCTS",
  "desc": "商品表",
  "fields": {
    "ID": {"type": "string", "pk": true, "desc": "商品ID"},
    "NAME": {"type": "string", "max": 100, "desc": "商品名称"},
    "DICT_CATEGORY": {"type": "string", "ref": "sys_dict_items.item_code", "desc": "分类"},
    "PRICE": {"type": "number", "desc": "价格"}
  }
}]);

const API_SPEC = `您的 API 规范内容...`; // 可直接复用您提供的规范
```

### 步骤 2：替换 Prompt 变量

```javascript
const prompt = TEMPLATE
  .replace('{{TABLE_INFO}}', TABLE_INFO)
  .replace('{{API_SPEC}}', API_SPEC)
  .replace('{{TABLE_NAME}}', 'PRODUCTS');
```

### 步骤 3：调用大模型

```javascript
const response = await callLLM(prompt);
const amisJSON = JSON.parse(response); // 直接得到可用 JSON
```

---

## 💡 增强建议（可选）

### 1. 添加字段类型映射规则
在 Prompt 中补充：
```markdown
## 字段组件映射
| 字段类型 | Amis 组件 | 额外配置 |
|---------|----------|---------|
| string + max | input-text | maxLength |
| string + ref | select | source 对接字典 |
| number | input-number | - |
| boolean | switch | - |
| date/datetime | input-datetime | - |
```

### 2. 添加校验规则生成
```markdown
## 表单校验
- `pk: true` 的字段：新增时 `required: true`, 编辑时 `disabled: true`
- `max` 字段：添加 `maxLength` 和 `validations: { maxLength: xxx }`
- 邮箱字段：添加 `validations: { isEmail: true }`
```

### 3. 添加国际化占位
```markdown
## 文案规范
- 按钮文案：`label: "${{TABLE_DESC}}管理"`
- 搜索 placeholder：`placeholder: "输入${{field.desc}}"`
```
