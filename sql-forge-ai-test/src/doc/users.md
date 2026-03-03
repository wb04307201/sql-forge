# 用户维护界面开发文档

> 本文档用于描述基于百度 Amis + SQL Forge API 搭建的用户维护界面，支持大模型根据本文档 + API接口规范 逆向生成原始界面JSON。

---

## 一、界面概述

| 属性 | 说明 |
|------|------|
| 界面类型 | 用户管理 CRUD 页面 |
| 核心组件 | `page` > `crud` (id: `crud_table`) |
| 主数据表 | `USERS` |
| 关联字典表 | `sys_dict_items` (字典编码: `sex`) |
| 主键字段 | `ID` |
| 展示字段 | `USERNAME`, `SEX`(字典名称), `EMAIL` |

### 功能清单
- ✅ 分页列表展示（支持排序、列显示控制）
- ✅ 组合条件搜索：用户名(LIKE)、性别(IN多选)、邮箱(LIKE)
- ✅ 新增用户（抽屉表单 + UUID主键）
- ✅ 修改用户（抽屉表单 + 数据预加载）
- ✅ 单条/批量删除（带确认）
- ✅ Excel导出（带筛选条件）
- ✅ 性别字段字典映射展示

---

## 二、数据模型定义

### 2.1 主表：USERS
```json
{
  "table": "USERS",
  "fields": {
    "ID": {"type": "string", "pk": true, "desc": "用户ID"},
    "USERNAME": {"type": "string", "max": 50, "desc": "用户名"},
    "DICT_SEX": {"type": "string", "max": 100, "desc": "性别字典code", "ref": "sys_dict_items.item_code"},
    "EMAIL": {"type": "string", "max": 100, "desc": "邮箱地址"}
  }
}
```

### 2.2 字典表：sys_dict_items
```json
{
  "table": "sys_dict_items",
  "fields": {
    "item_code": {"type": "string", "desc": "字典项值"},
    "item_name": {"type": "string", "desc": "字典项标签"},
    "dict_code": {"type": "string", "desc": "字典分类编码"}
  },
  "filter": {"dict_code": "sex"}
}
```

### 2.3 字段映射关系
| 界面字段 | 数据库字段 | 说明 |
|---------|-----------|------|
| `SEX` (展示) | `sex.item_name` | 列表/搜索中显示性别名称 |
| `DICT_SEX` (存储) | `USERS.DICT_SEX` | 表单中提交性别code |
| `SEX` (表单name) | `USERS.DICT_SEX` | ⚠️ 修改表单中name为`SEX`但实际绑定`DICT_SEX`字段 |

---

## 三、API 调用规范（基于 SQL Forge）

> 基础规范：`POST /sql/forge/api/json/{method}/{tableName}`

### 3.1 接口清单

| 功能 | method | tableName | 关键参数 | 说明 |
|------|--------|-----------|---------|------|
| 分页查询 | `selectPage` | `USERS` | `@column`, `@where`, `@join`, `@page`, `@order` | 列表主接口 |
| 单条查询 | `select` | `USERS` | `@where: ID=EQ` | 编辑时加载数据 |
| 新增 | `insert` | `USERS` | `@set` | 插入新用户 |
| 修改 | `update` | `USERS` | `@set`, `@where: ID=EQ` | 更新用户信息 |
| 删除 | `delete` | `USERS` | `@where: ID=EQ/IN` | 单条/批量删除 |
| 字典加载 | `select` | `sys_dict_items` | `@where: dict_code=EQ` | 性别下拉源 |
| 导出 | `select` | `USERS` | 同分页查询(无@page) | 导出当前筛选结果 |

### 3.2 关键请求体模板

#### 🔹 分页查询（列表）
```json
{
  "@column": ["USERS.ID", "USERS.USERNAME", "sex.item_name as SEX", "USERS.EMAIL"],
  "@join": [{
    "type": "LEFT_OUTER_JOIN",
    "joinTable": "sys_dict_items sex",
    "on": "USERS.DICT_SEX = sex.item_code"
  }],
  "@where": [
    {"column": "USERS.USERNAME", "condition": "LIKE", "value": "${USERNAME | default:undefined}"},
    {"column": "USERS.DICT_SEX", "condition": "IN", "value": "${SEX | default:undefined | split}"},
    {"column": "USERS.EMAIL", "condition": "LIKE", "value": "${EMAIL | default:undefined}"},
    {"column": "sex.dict_code", "condition": "EQ", "value": "sex"}
  ],
  "@order": ["${default(orderBy && orderDir ? (orderBy + ' ' + orderDir):'',undefined)}"],
  "@page": {"pageIndex": "${page - 1}", "pageSize": "${perPage}"}
}
```

#### 🔹 新增用户
```json
{
  "@set": {
    "ID": "${ID | default:undefined}",
    "USERNAME": "${USERNAME | default:undefined}",
    "DICT_SEX": "${DICT_SEX | default:undefined}",
    "EMAIL": "${EMAIL | default:undefined}"
  }
}
```

#### 🔹 修改用户（更新）
```json
{
  "@set": {
    "ID": "${ID}",
    "USERNAME": "${USERNAME}",
    "SEX": "${SEX}",
    "EMAIL": "${EMAIL}"
  },
  "@where": [{"column": "USERS.ID", "condition": "EQ", "value": "${ID}"}]
}
```
> ⚠️ 注意：修改表单中字段name为`SEX`，但实际更新的是`USERS.DICT_SEX`字段

#### 🔹 删除（单条/批量）
```json
{
  "@where": [{
    "column": "ID",
    "condition": "${批量 ? 'IN' : 'EQ'}",
    "value": "${批量 ? ids : ID} | split"
  }]
}
```

#### 🔹 字典数据源
```json
{
  "@column": ["item_code", "item_name"],
  "@where": [{"column": "dict_code", "condition": "EQ", "value": "sex"}]
}
```
前端 adaptor 转换：
```javascript
return {
  options: payload.map(item => ({
    value: item.item_code || item.ITEM_CODE,
    label: item.item_name || item.ITEM_NAME
  }))
};
```

---

## 四、Amis 组件配置详解

### 4.1 CRUD 核心配置
```json
{
  "type": "crud",
  "id": "crud_table",
  "primaryField": "ID",
  "autoGenerateFilter": true,
  "keepItemSelectionOnPageChange": true,
  "columns": [
    {"name": "ID", "hidden": true},
    {
      "name": "USERNAME",
      "label": "用户名",
      "sortable": true,
      "searchable": {"type": "input-text", "name": "USERNAME", "maxLength": 50}
    },
    {
      "name": "SEX",
      "label": "性别",
      "sortable": true,
      "searchable": {
        "type": "select",
        "name": "SEX",
        "multiple": true,
        "source": {"url": "/sql/forge/api/json/select/sys_dict_items", "...": "..."}
      }
    },
    {
      "name": "EMAIL",
      "label": "用户邮箱地址",
      "sortable": true,
      "searchable": {"type": "input-text", "name": "EMAIL", "maxLength": 100}
    },
    {
      "type": "operation",
      "label": "操作",
      "fixed": "right",
      "buttons": [/* 修改/删除按钮 */]
    }
  ]
}
```

### 4.2 抽屉表单配置要点

#### 新增表单
- `ID` 字段使用 `type: "uuid"` 自动生成
- 性别字段 `name: "DICT_SEX"` 绑定数据库存储字段
- 提交成功后 `actionType: "reload"` 刷新 `crud_table`

#### 修改表单
- `initApi` 预加载当前记录，`responseData: "&": "${items | first}"` 提取第一条
- ⚠️ `@join` 中关联表别名可能动态生成（如 `sex_a814d446`），需保持与 `@column` 一致
- 表单字段 `name: "SEX"` 但实际更新 `USERS.DICT_SEX`

### 4.3 表达式与变量说明
| 表达式 | 说明 |
|--------|------|
| `${page - 1}` | Amis页码(1起始) → API页码(0起始) 转换 |
| `${SEX \| default:undefined \| split}` | 多选值转数组，空值转undefined避免SQL错误 |
| `${default(orderBy && orderDir ? (orderBy + ' ' + orderDir):'',undefined)}` | 动态排序字段，空时传undefined |
| `${ids \| split}` | 批量操作时ID数组转逗号分隔字符串 |

---

## 五、⚠️ 注意事项与常见坑点

1. **字段名不一致**：列表展示`SEX`(字典名称)，表单存储`DICT_SEX`(字典code)，修改表单中`name: "SEX"`但实际绑定`DICT_SEX`
2. **关联表别名**：修改表单`initApi`中`@join`的别名可能动态生成（如`sex_a814d446`），需确保`@column`中使用相同别名
3. **导出接口表名错误**：示例中`sys_dict_item`应为`sys_dict_items`（少`s`），实际使用时需修正
4. **分页索引转换**：Amis的`page`从1开始，API的`pageIndex`从0开始，必须`${page - 1}`
5. **多选搜索值处理**：必须使用`| split`过滤器将逗号分隔字符串转为数组供`IN`条件使用
6. **字典源适配器**：必须处理大小写兼容`item_code/ITEM_CODE`，避免后端返回字段名大小写不一致导致绑定失败

---

## 六、逆向生成提示语模板

> 将此提示语 + 本文档 + API接口规范 发送给大模型，可逆向生成原始界面JSON

```markdown
# 任务：根据开发文档逆向生成百度 Amis 界面 JSON

## 输入信息
1. 【API接口规范】：SQL Forge JSON-to-SQL 接口规范（select/selectPage/insert/update/delete）
2. 【开发文档】：用户维护界面开发文档（含数据模型、API清单、组件配置、注意事项）

## 输出要求
生成完整的 Amis page JSON，满足：
✅ 使用 crud 组件(id: crud_table)展示 USERS 表数据
✅ 支持分页、排序、列显示控制、组合搜索(用户名/性别/邮箱)
✅ 新增/修改使用 drawer 抽屉表单，字段：ID(uuid)/USERNAME/DICT_SEX(字典)/EMAIL
✅ 性别字段：列表展示字典名称(SEX)，表单存储字典code(DICT_SEX)
✅ 删除支持单条+批量，带确认提示
✅ 导出Excel携带当前筛选条件
✅ 所有API调用符合 SQL Forge 规范，使用@column/@where/@join/@page等参数
✅ 处理关键表达式：${page-1}、${xxx|split}、default()等
✅ 字典源使用adaptor转换item_code/item_name为value/label

## 特别约束
⚠️ 修改表单initApi的@join别名可能动态生成，需保持@column引用一致
⚠️ 表单字段name与数据库字段映射：SEX表单name → DICT_SEX数据库字段
⚠️ 分页pageIndex从0开始，必须转换
⚠️ 多选搜索值必须用|split处理

## 输出格式
直接输出完整JSON，无需额外说明，确保可直接粘贴到 Amis 编辑器运行。
```

---

## 七、验证清单（生成后自查）

- [ ] CRUD组件id为`crud_table`，primaryField为`ID`
- [ ] 列表查询API使用`selectPage`，pageIndex正确转换
- [ ] 性别搜索为`multiple: true`的select，值用`|split`处理
- [ ] 新增表单ID字段为`type: "uuid"`
- [ ] 修改表单initApi使用`responseData: {"&": "${items | first}"}`
- [ ] 所有字典源配置adaptor转换逻辑
- [ ] 操作列按钮fixed: "right"
- [ ] 批量删除API条件使用`IN` + `|split`
- [ ] 导出接口无@page参数，其他条件与列表查询一致

---

> 💡 使用建议：将本开发文档 + API接口规范 + 逆向提示语 三者组合，可确保大模型生成的JSON与原始界面高度一致。如遇字段映射问题，重点检查`SEX`/`DICT_SEX`的name绑定关系。