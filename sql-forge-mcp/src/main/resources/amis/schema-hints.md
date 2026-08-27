# Amis Schema 速查手册

> 本文档为 sql-forge-mcp 模块中 AI Agent 构造 Amis 页面 JSON Schema 的速查参考。
> 所有内容均从实际生成经验中提炼，**优先使用本手册**而非即兴生成。

---

## 1. 整体结构

Amis 页面 JSON 是一个嵌套树，最外层必须是 `page`：

```json
{
  "type": "page",
  "title": "页面标题",
  "body": { "type": "...", "...": "..." }
}
```

`body` 可以是单个组件对象，也可以是数组（数组里任意位置都是组件对象，按顺序渲染）。

---

## 2. 表达式 `${...}`

Amis 内嵌表达式语法 `${expression}`，常用场景：

```json
{
  "label": "用户：${name}",
  "hiddenOn": "${status !== 'ON'}",          // 隐藏条件
  "visibleOn": "${age >= 18}",               // 显示条件
  "disabledOn": "${!agreed}",                // 禁用条件
  "value": "${id}",                          // 默认值
  "tpl": "<div>姓名：$name<br/>邮箱：$email</div>"
}
```

支持运算符：`+ - * / % == != > >= < <= && || !`、三元 `?:`、括号。

---

## 3. API 字段写法

Amis 中所有 `api` 字段都是相对 URL（基于 host），fetcher 会拼上 baseUrl。

**三种写法**：

```json
// 1) 直接写路径
"api": "POST /sql/forge/api/json/select/users"

// 2) 写对象（带配置）
"api": {
  "url": "/sql/forge/api/json/insert/users",
  "method": "post",
  "data": { "@set": { "name": "${name}" } },
  "headers": { "X-Api-Key": "test" }
}

// 3) 完整对象（含 dataAdapter 等）
"api": {
  "method": "post",
  "url": "/sql/forge/api/json/insert/users",
  "dataType": "json",
  "responseData": { "id": "${id}" }
}
```

**注意**：SQL Forge 后端的 JSON CRUD 路径固定为（**全部 POST**，select 也要带 body）：
- `POST /sql/forge/api/json/select/{table}` — 查询（body 含 `@where` 等条件）
- `POST /sql/forge/api/json/selectPage/{table}` — 分页查询
- `POST /sql/forge/api/json/insert/{table}` — 插入（body 含 `@set`）
- `POST /sql/forge/api/json/update/{table}` — 更新（body 含 `@set` + `@where`）
- `POST /sql/forge/api/json/delete/{table}` — 删除（body 含 `@where`）

**responseData 转换**：Amis 默认期望响应 `{status:0, msg:"", data:...}`，status=0 表示成功。

如果后端响应不是这种结构，需要在 api 中配 `responseData`：
```json
"api": "POST /sql/forge/api/json/selectPage/users",
"responseData": {
  "rows": "${rows}",
  "total": "${total}"
}
```

---

## 4. CRUD 套路

标准 CRUD 页面三件套：

```json
{
  "type": "crud",
  "api": "POST /sql/forge/api/json/selectPage/users",       // 列表
  "perPage": 20,
  "filter": { "title": "筛选", "body": [...] }, // 筛选
  "headerToolbar": [                             // 顶部
    "bulkActions",
    { "type": "button", "label": "新增", "level": "primary", "actionType": "dialog", "dialog": {...} }
  ],
  "columns": [
    { "name": "id", "label": "ID", "sortable": true },
    {
      "type": "operation",
      "label": "操作",
      "buttons": [
        { "type": "button", "label": "编辑", "actionType": "dialog", "dialog": {...} },
        { "type": "button", "label": "删除", "actionType": "ajax", "api": "POST /sql/forge/api/json/delete/users", "confirmText": "确认删除？" }
      ]
    }
  ]
}
```

**关键点**：
- `selectPage` 返回 `{total: Long, rows: [...]}`，Crud 组件默认能解析
- 行内编辑必须配 `quickSaveApi` 或 `quickSaveItemApi`
- 筛选字段名须与 `@column` 字段名一致才能查询生效

---

## 5. Form 三种用法

### 5.1 独立表单页
```json
{ "type": "page", "body": { "type": "form", "api": "...", "body": [...] } }
```
适合大表单或独立编辑场景。

### 5.2 弹窗表单
```json
{
  "type": "button",
  "actionType": "dialog",
  "dialog": {
    "title": "新增",
    "body": { "type": "form", "api": "POST /sql/forge/api/json/insert/users", "body": [...] }
  }
}
```

### 5.3 抽屉表单
```json
{
  "type": "button",
  "actionType": "drawer",
  "drawer": {
    "title": "详情",
    "position": "right",
    "body": { "type": "form", "api": "POST /sql/forge/api/json/update/users", "body": [...] }
  }
}
```

### 5.4 表单项分组
```json
{
  "type": "form",
  "body": [
    {
      "type": "group",
      "body": [
        {"type": "input-text", "name": "firstName", "label": "名", "md": 6},
        {"type": "input-text", "name": "lastName", "label": "姓", "md": 6}
      ]
    }
  ]
}
```

---

## 6. Dialog / Drawer 踩坑

### Dialog
- `title` 必填
- `body` 必填（通常是 form）
- 关闭后表单数据会自动重置（除非 `data` 是动态绑定）
- 嵌套 CRUD 时**不要**在 dialog 内放完整 CRUD，嵌套层级过深会导致布局错乱

### Drawer
- `position` 默认 `right`，可选 `left / top / bottom`
- `size` 控制宽/高（小屏可占 100%）
- 在抽屉内嵌套 form 时，form 的 `redirect` 失效（抽屉关闭后无法跳转），应使用 `actions` 自定义提交按钮

---

## 7. 数据库类型 → 表单项映射

| 数据库类型 | 推荐 Amis 表单组件 | 备注 |
|---|---|---|
| VARCHAR/TEXT | `input-text` 或 `textarea` | 长字段用 textarea |
| INT/BIGINT/DECIMAL | `input-number` | 配置 `precision` 控制小数位 |
| DATE | `input-date` | `format: YYYY-MM-DD` |
| DATETIME/TIMESTAMP | `input-datetime` | `format: YYYY-MM-DD HH:mm:ss` |
| BOOLEAN/TINYINT(1) | `switch` | |
| ENUM | `select` 或 `radios` | 枚举值少用 radios |
| 主键 ID | `hidden` | 编辑时携带 id，新增不出现 |
| 外键 | `select` + `source` 远程选项 | `source: "POST /sql/forge/api/json/select/parent_table"` |

---

## 8. SQL Forge 后端调用约定

### 8.1 JSON CRUD body 写法

**Select**（注意：方法为 POST）：
```json
{
  "@column": ["id", "name", "email"],
  "@where": [
    {"column": "status", "condition": "EQ", "value": "ON"},
    {"column": "age", "condition": "GTEQ", "value": 18}
  ],
  "@order": ["created_at DESC"],
  "@join": [],
  "@group": [],
  "@distinct": false
}
```

**SelectPage**：
```json
{
  "@column": ["id", "name"],
  "@where": [...],
  "@page": { "pageIndex": 0, "pageSize": 20 },
  "@order": ["id DESC"]
}
```

**Insert**：
```json
{ "@set": { "name": "张三", "email": "a@b.com" }, "@with_select": null }
```

**Update**：
```json
{ "@set": { "status": "OFF" }, "@where": [{"column":"id","condition":"EQ","value":"1"}] }
```

**Delete**：
```json
{ "@where": [{"column":"id","condition":"EQ","value":"1"}] }
```

### 8.2 条件运算符

| 取值 | 含义 |
|---|---|
| EQ / NOT_EQ | 等于 / 不等于 |
| GT / LT / GTEQ / LTEQ | 大于 / 小于 / 大于等于 / 小于等于 |
| LIKE / NOT_LIKE / LEFT_LIKE / RIGHT_LIKE | LIKE 匹配 |
| BETWEEN / NOT_BETWEEN | 区间 |
| IN / NOT_IN | 包含 |
| IS_NULL / IS_NOT_NULL | 空判断 |

---

## 9. 校验规则

### 9.1 表单整体校验
```json
{
  "type": "form",
  "rules": [
    {"name": "name", "required": true, "message": "姓名必填"},
    {"name": "email", "type": "email", "message": "邮箱格式错误"}
  ],
  "messages": {
    "required": "${label}不能为空"
  }
}
```

### 9.2 单项校验
```json
{
  "type": "input-text",
  "name": "email",
  "label": "邮箱",
  "required": true,
  "validations": {
    "isEmail": true,
    "maxLength": 100
  },
  "validationErrors": {
    "isEmail": "请输入正确的邮箱格式",
    "maxLength": "邮箱最长 100 字符"
  }
}
```

内置校验：isEmail、isUrl、isNumeric、isAlpha、isAlphanumeric、isInt、isFloat、isLength、maxLength、minLength、matchRegexp、isJson 等。

---

## 10. 常用模板片段

### 10.1 操作列带删除确认
```json
{
  "type": "operation",
  "label": "操作",
  "buttons": [
    {
      "type": "button",
      "label": "删除",
      "actionType": "ajax",
      "level": "danger",
      "api": "POST /sql/forge/api/json/delete/users",
      "data": { "@where": [{"column": "id", "condition": "EQ", "value": "${id}"}] },
      "confirmText": "确认删除？"
    }
  ]
}
```

### 10.2 行内编辑
```json
{
  "name": "enabled",
  "label": "状态",
  "type": "switch",
  "quickEdit": true,
  "quickEditOnUpdate": "POST /sql/forge/api/json/update/users"
}
```

### 10.3 远程下拉选项
```json
{
  "type": "select",
  "name": "userId",
  "label": "用户",
  "source": "POST /sql/forge/api/json/select/users",
  "valueField": "id",
  "labelField": "name"
}
```

---

## 11. 错误排查清单

| 现象 | 原因 | 修复 |
|---|---|---|
| 页面一片空白 | type 字段缺失或拼错 | 确认每个组件都有 `type` |
| 表格不显示 | `dataSource` 未填或路径错 | 用 `${rows}` 或 `${items}` 引用 |
| 按钮点了没反应 | `actionType` 拼错 | 应为 button / submit / reset / url / dialog / drawer / ajax |
| 表单提交报错 | `api` 路径错 | 检查 method + path |
| 操作列按钮错位 | `type: "operation"` 拼成 `operation-column` | 操作用 `operation` 类型 |
| select 显示 [object] | 没配 `labelField` | select 必须配 valueField + labelField |
| 日期显示 1970 | `format` 与 `valueFormat` 冲突 | 只设 valueFormat 让 amis 自动展示 |

---

## 12. 版本与依赖

- Amis SDK：`amis@6.12.0`（jsdelivr CDN）
- 渲染器：headless Chromium via Playwright
- 后端：SQL Forge JSON CRUD（任何兼容端点都可）