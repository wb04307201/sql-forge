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
```json
[{
  "table": "USERS",
  "desc": "用户表",
  "fields": {
    "ID": {"type": "string", "pk": true, "desc": "用户ID"},
    "USERNAME": {"type": "string", "max": 50, "desc": "用户名"},
    "DICT_SEX": {"type": "string", "max": 100, "desc": "性别", "ref": {
      "table": "sys_dict_items",
      "on": "item_code",
      "filter": {"dict_code": "sex"}
    }},
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
  }
}]
```

## 2️⃣ 后端 API 规范摘要
让前端无需编写后端代码即可操作数据库，通过`JSON`格式描述自己需要的数据结构和操作，后端自动生成对应的`SQL`执行并返回结果。

- **请求路径**: `/sql/forge/api/json/{method}/{tableName}?executorName={executorName}`
- **请求方法**: `POST`
- **内容类型**: `application/json`
- **路径参数**:
  - `{method}`: 操作方法类型(select、selectPage、insert、update、delete)
  - `{tableName}`: `表名`或者`表名 别名`
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
##### 查询
###### 请求
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

###### 生成的SQL
```sql
SELECT u.username, sex.item_name             AS sex_name, o.total_amount, p.name               AS product_name, categories.item_name AS product_categories, oi.unit_price, oi.quantity, p.price
FROM orders o
JOIN users u ON o.user_id = u.id
JOIN sys_dict_items sex ON u.dict_sex = sex.item_code
JOIN order_items oi ON o.id = oi.order_id
JOIN products p ON oi.product_id = p.id
JOIN sys_dict_items categories ON p.dict_categories = categories.item_code
WHERE (sex.dict_code = ? AND categories.dict_code = ?)
ORDER BY o.order_date
```
###### 生成的参数
```json
{
  1: "sex",
  2: "categories"
}
```

##### 分页查询
###### 请求
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
###### 生成的SQL
```sql
SELECT u.username, sex.item_name             AS sex_name, o.total_amount, p.name               AS product_name, categories.item_name AS product_categories, oi.unit_price, oi.quantity, p.price
FROM orders o
JOIN users u ON o.user_id = u.id
JOIN sys_dict_items sex ON u.dict_sex = sex.item_code
JOIN order_items oi ON o.id = oi.order_id
JOIN products p ON oi.product_id = p.id
JOIN sys_dict_items categories ON p.dict_categories = categories.item_code
WHERE (sex.dict_code = ? AND categories.dict_code = ?)
ORDER BY o.order_date LIMIT ? OFFSET ?
```

###### 生成的参数
```json
{
  1: "sex",
  2: "categories",
  3: 5,
  4: 0
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
    "dict_sex": "female",
    "email": "wb04307201@gitee.com"
  }
}
```

###### 生成的SQL
```sql
INSERT INTO users
  (id, username, dict_sex, email)
VALUES (?, ?, ?, ?)
```

###### 生成的参数
```json
{
  1: "26a05ba3-913d-4085-a505-36d40021c8d1",
  2: "wb04307201",
  3: "female",
  4: "wb04307201@gitee.com"
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
  ]
}
```

###### 生成的SQL
```sql
UPDATE users
SET email = ?
WHERE (id = ?)
```

###### 生成的参数
```json
{
  1: "wb04307201@github.com",
  2: "26a05ba3-913d-4085-a505-36d40021c8d1"
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
  ]
}
```

###### 生成的SQL
```sql
DELETE FROM users
WHERE (id = ?)
```

###### 生成的参数
```json
{
  1: "26a05ba3-913d-4085-a505-36d40021c8d1"
}
```

#### 方法执行前切面
可通过实现[IExecute.java](sql-forge-crud/src/main/java/cn/wubo/sql/forge/inter/IExecute.java)接口自定义方法执行前的json调整，实现密码加密、自动更新时间戳、权限控制、日志、审计等

例如实现在Insert时输出日志：
```java
@Slf4j
@Component
public class LogInsertExecute implements IBeforeRecordExecutor<Insert> {
  @Override
  public Boolean support(String tableName, Insert insert) {
    return true;
  }

  @Override
  public Insert before(String tableName, Insert insert) {
    log.info("LogInsertExecute tableName: {} record: {}", tableName, insert);
    return insert;
  }
}
```

#### 配置
可通过`sql.forge.api.json.enabled=false`关闭

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