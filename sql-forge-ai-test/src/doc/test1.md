# 角色设定
你是一名资深前端工程师，精通百度 Amis 低代码框架和 RESTful API 集成。你的任务是根据提供的「表结构信息」和「API 规范」，生成一个功能完整的单表维护 Amis 界面 JSON。

# 任务目标
生成一个支持【分页查询 + 新增 + 编辑 + 删除 + 批量删除 + 导出 + 字典关联 + 条件筛选】的 Amis CRUD 页面 JSON。

# 输入信息

## 1️⃣ 表结构信息
```json
[{
  "table": "USERS",
  "desc": "用户表",
  "fields": {
    "ID": {"type": "string", "pk": true, "desc": "用户ID"},
    "USERNAME": {"type": "string", "max": 50, "desc": "用户名"},
    "DICT_SEX": {"type": "string", "max": 100, "desc": "性别", "ref": "sys_dict_items.item_code"},
    "EMAIL": {"type": "string", "max": 100, "desc": "邮箱地址"}
  }
},

{
  "table": "sys_dict_items",
  "desc": "字典项表",
  "fields": {
    "item_code": {"type": "string", "desc": "字典项编码"},
    "dict_code": {"type": "string", "desc": "字典项编码"},
    "item_name": {"type": "string", "desc": "字典项名称"}
  },
  "filter": {"dict_code": "sex"}
}]
```

## 2️⃣ API 规范
让前端无需编写后端代码即可操作数据库，通过`JSON`格式描述自己需要的数据结构和操作，后端自动生成对应的`SQL`执行并返回结果。

- **请求路径**: `/sql/forge/api/json/{method}/{tableName}?executorName={executorName}`
- **请求方法**: `POST`
- **内容类型**: `application/json`
- **路径参数**:
  - `{method}`: 操作方法类型(select、selectPage、insert、update、delete)
  - `{tableName}`: 数据库表名称
  - `{executorName}`: 数据库执行器名称,默认支持database(项目数据库),calcite(Apache Calcite跨数据库联邦查询)，支持自行扩展，如不传，默认使用database

#### select 方法

##### 请求格式
```json
{
  "@column": ["字段名1", "字段名2"],
  "@where": [
    {
      "column": "字段名",
      "condition": "条件类型",
      "value": "值"
    }
  ],
  "@join": [
    {
      "type": "JOIN类型",
      "joinTable": "关联表名",
      "on": "关联条件"
    }
  ],
  "@order": ["字段名 ASC", "字段名 DESC"],
  "@group": ["字段名"],
  "@distince": false
}
```

###### 参数说明
- `@column`: 要查询的字段数组，为空则查询所有字段
- `@where`: 查询条件数组
- `@join`: 关联查询条件数组
- `@order`: 排序字段数组
- `@group`: 分组字段数组
- `@distince`: 是否去重

#### selectPage 方法

##### 请求格式
```json
{
  "@column": ["字段名1", "字段名2"],
  "@where": [
    {
      "column": "字段名",
      "condition": "条件类型",
      "value": "值"
    }
  ],
  "@page": {
    "pageIndex": 0,
    "pageSize": 10
  },
  "@join": [
    {
      "type": "JOIN类型",
      "joinTable": "关联表名",
      "on": "关联条件"
    }
  ],
  "@order": ["字段名 ASC", "字段名 DESC"],
  "@distince": false
}
```

##### 参数说明
- `@column`: 要查询的字段数组，为空则查询所有字段
- `@where`: 查询条件数组
- `@page`分页参数
  - pageIndex: 页码（从0开始）
  - pageSize: 每页大小
- `@join`: 关联查询条件数组
- `@order`: 排序字段数组
- `@distince`: 是否去重

#### insert 方法

##### 请求格式
```json
{
  "@set": {
    "字段名1": "值1",
    "字段名2": "值2"
  },
  "@with_select": {
    // 插入后查询json
  }
}
```

##### 参数说明
- `@set`: 要插入的字段和值的键值对，至少需要一个字段
- `@with_select`: 可选的查询条件，用于插入后执行一个查询

#### update 方法

##### 请求格式
```json
{
  "@set": {
    "字段名1": "新值1",
    "字段名2": "新值2"
  },
  "@where": [
    {
      "column": "字段名",
      "condition": "条件类型",
      "value": "值"
    }
  ],
  "@with_select": {
    // 更新后查询json
  }
}
```

##### 参数说明
- `@set`: 要更新的字段和新值的键值对，至少需要一个字段
- `@where`: 更新条件数组，指定要更新哪些记录
- `@with_select`: 可选的查询条件，用于更新后执行一个查询

#### delete 方法

##### 请求格式
```json
{
  "@where": [
    {
      "column": "字段名",
      "condition": "条件类型",
      "value": "值"
    }
  ],
  "@with_select": {
    // 删除后查询json
  }
}
```

##### 参数说明
- `@where`: 删除条件数组，每个条件包含：
  - column: 要匹配的字段名
  - condition: 条件类型（EQ、NOT_EQ、GT、LT、GTEQ、LTEQ、LIKE、NOT_LIKE、LEFT_LIKE、RIGHT_LIKE、BETWEEN、NOT_BETWEEN、IN、NOT_IN、IS_NULL、IS_NOT_NULL）
  - value: 匹配的值
- `@with_select`: 可选的查询条件，用于在删除后执行一个查询

#### 示例
1. 查询
```http request
POST http://localhost:8080/sql/forge/api/json/select/orders o
Content-Type: application/json

{
  "@column": [
    "u.username",
    "sex.item_name             AS sex_name",
    "o.total_amount",
    "p.name               AS product_name",
    "categories.item_name AS product_categories",
    "oi.unit_price",
    "oi.quantity",
    "p.price"
  ],
  "@where": [
    {
      "column": "sex.dict_code",
      "condition": "EQ",
      "value": "sex"
    },
    {
      "column": "categories.dict_code",
      "condition": "EQ",
      "value": "categories"
    }
  ],
  "@join": [
    {
      "type": "JOIN",
      "joinTable": "users u",
      "on": "o.user_id = u.id"
    },
    {
      "type": "JOIN",
      "joinTable": "sys_dict_items sex",
      "on": "u.dict_sex = sex.item_code"
    },
    {
      "type": "JOIN",
      "joinTable": "order_items oi",
      "on": "o.id = oi.order_id"
    },
    {
      "type": "JOIN",
      "joinTable": "products p",
      "on": "oi.product_id = p.id"
    },
    {
      "type": "JOIN",
      "joinTable": "sys_dict_items categories",
      "on": "p.dict_categories = categories.item_code"
    }
  ],
  "@order": [
    "o.order_date"
  ],
  "@group": null,
  "@distince": false
}
```

2. 分页查询
```http request
POST http://localhost:8080/sql/forge/api/json/selectPage/orders o
Content-Type: application/json

{
  "@column": [
    "u.username",
    "sex.item_name             AS sex_name",
    "o.total_amount",
    "p.name               AS product_name",
    "categories.item_name AS product_categories",
    "oi.unit_price",
    "oi.quantity",
    "p.price"
  ],
  "@where": [
    {
      "column": "sex.dict_code",
      "condition": "EQ",
      "value": "sex"
    },
    {
      "column": "categories.dict_code",
      "condition": "EQ",
      "value": "categories"
    }
  ],
  "@join": [
    {
      "type": "JOIN",
      "joinTable": "users u",
      "on": "o.user_id = u.id"
    },
    {
      "type": "JOIN",
      "joinTable": "sys_dict_items sex",
      "on": "u.dict_sex = sex.item_code"
    },
    {
      "type": "JOIN",
      "joinTable": "order_items oi",
      "on": "o.id = oi.order_id"
    },
    {
      "type": "JOIN",
      "joinTable": "products p",
      "on": "oi.product_id = p.id"
    },
    {
      "type": "JOIN",
      "joinTable": "sys_dict_items categories",
      "on": "p.dict_categories = categories.item_code"
    }
  ],
  "@order": [
    "o.order_date"
  ],
  "@group": null,
  "@distince": false,
  "@page": {
    "pageIndex": 0,
    "pageSize": 5
  }
}
```

3. 插入
```http request
POST http://localhost:8080/sql/forge/api/json/insert/users
Content-Type: application/json

{
  "@set": {
    "id": "26a05ba3-913d-4085-a505-36d40021c8d1",
    "username": "wb04307201",
    "email": "wb04307201@gitee.com"
  },
  "@with_select": {
    "@column": null,
    "@where": [
      {
        "column": "id",
        "condition": "EQ",
        "value": "26a05ba3-913d-4085-a505-36d40021c8d1"
      }
    ],
    "@join": null,
    "@order": null,
    "@group": null,
    "@distince": false
  }
}
```

4. 更新
```http request
POST http://localhost:8080/sql/forge/api/json/update/users
Content-Type: application/json

{
  "@set": {
    "email": "wb04307201@github.com"
  },
  "@where": [
    {
      "column": "id",
      "condition": "EQ",
      "value": "26a05ba3-913d-4085-a505-36d40021c8d1"
    }
  ],
  "@with_select": {
    "@column": null,
    "@where": [
      {
        "column": "id",
        "condition": "EQ",
        "value": "26a05ba3-913d-4085-a505-36d40021c8d1"
      }
    ],
    "@join": null,
    "@order": null,
    "@group": null,
    "@distince": false
  }
}
```

5. 删除
```http request
POST http://localhost:8080/sql/forge/api/json/delete/users
Content-Type: application/json

{
  "@where": [
    {
      "column": "id",
      "condition": "EQ",
      "value": "26a05ba3-913d-4085-a505-36d40021c8d1"
    }
  ],
  "@with_select": {
    "@column": null,
    "@where": [
      {
        "column": "id",
        "condition": "EQ",
        "value": "26a05ba3-913d-4085-a505-36d40021c8d1"
      }
    ],
    "@join": null,
    "@order": null,
    "@group": null,
    "@distince": false
  }
}
```

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
请根据以上要求，为表 `users` 生成 Amis 界面 JSON：