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
```json
[{
  "table": "products",
  "desc": "商品表",
  "type": "table",
  "fields": {
    "ID": {"type": "string", "pk": true, "desc": "商品ID"},
    "name": {"type": "string", "max": 50, "desc": "商品名称","search": true},
    "dict_categories": {"type": "string", "max": 100, "desc": "商品类型", "ref": {"type":"JOIN","table": "sys_dict_items", "on": "item_code", "filter": {"dict_code": "categories"}},"search": true},
    "price": {"type": "number", "max": 10, "precision": 2, "desc": "邮箱地址","search": true}
  }
},
{
  "table": "sys_dict_items",
  "desc": "字典项表",
  "type": "ref",
  "fields": {
    "item_code": {"type": "string", "desc": "字典项编码"},
    "dict_code": {"type": "string", "desc": "字典项编码"},
    "item_name": {"type": "string", "desc": "字典项名称"}
  }
}]
```

## 2️⃣ API 规范（关键摘要）
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
  - type: JOIN类型（JOIN, INNER_JOIN, LEFT_OUTER_JOIN, RIGHT_OUTER_JOIN, OUTER_JOIN）
  - joinTable: 关联表名
  - on: 关联条件
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
| 表字段类型                | Amis 组件        | 搜索组件                 | 备注                       |
|----------------------|----------------|----------------------|--------------------------|
| string + search:true | input-text     | input-text + LIKE    | maxLength 取自字段 max       |
| string + ref(字典)     | select         | select + 多选          | source 调用 sys_dict_items |
| number               | input-number   | input-number         | -                        |
| date/datetime        | input-datetime | input-datetime-range | -                        |
| boolean              | switch         | -                    | -                        |
| 主键                   | hidden         | -                    | 新增时用 uuid 组件生成           |

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

# 示例
## 表结构信息（JSON）
```json
[{
  "table": "USERS",
  "desc": "用户表",
  "type": "table",
  "fields": {
    "ID": {"type": "string", "pk": true, "desc": "用户ID"},
    "USERNAME": {"type": "string", "max": 50, "desc": "用户名","search": true},
    "DICT_SEX": {"type": "string", "max": 100, "desc": "性别", "ref": {"type":"JOIN","table": "sys_dict_items", "on": "item_code", "filter": {"dict_code": "sex"}},"search": true},
    "EMAIL": {"type": "string", "max": 100, "desc": "邮箱地址","search": true}
  }
},
  {
    "table": "sys_dict_items",
    "desc": "字典项表",
    "type": "ref",
    "fields": {
      "item_code": {"type": "string", "desc": "字典项编码"},
      "dict_code": {"type": "string", "desc": "字典项编码"},
      "item_name": {"type": "string", "desc": "字典项名称"}
    }
  }]
```

## Amis CRUD 页面 JSON 配置
```json
{
  "type": "page",
  "body": {
    "type": "crud",
    "id": "crud_table",
    "api": {
      "method": "post",
      "url": "/sql/forge/api/json/selectPage/USERS",
      "data": {
        "@column": [
          "USERS.ID",
          "USERS.USERNAME",
          "sex.item_name as SEX",
          "USERS.EMAIL"
        ],
        "@join": [
          {
            "type": "JOIN",
            "joinTable": "sys_dict_items sex",
            "on": "USERS.DICT_SEX = sex.item_code"
          }
        ],
        "@where": [
          {
            "column": "USERS.USERNAME",
            "condition": "LIKE",
            "value": "${USERNAME | default:undefined}"
          },
          {
            "column": "USERS.DICT_SEX",
            "condition": "IN",
            "value": "${SEX | default:undefined | split}"
          },
          {
            "column": "USERS.EMAIL",
            "condition": "LIKE",
            "value": "${EMAIL | default:undefined}"
          },
          {
            "column": "sex.dict_code",
            "condition": "EQ",
            "value": "sex"
          }
        ],
        "@order": [
          "${default(orderBy && orderDir ? (orderBy + ' ' + orderDir):'',undefined)}"
        ],
        "@page": {
          "pageIndex": "${page - 1}",
          "pageSize": "${perPage}"
        }
      }
    },
    "headerToolbar": [
      {
        "label": "新增",
        "type": "button",
        "icon": "fa fa-plus",
        "level": "primary",
        "actionType": "drawer",
        "drawer": {
          "title": "新增表单",
          "body": {
            "type": "form",
            "api": {
              "method": "post",
              "url": "/sql/forge/api/json/insert/USERS",
              "data": {
                "@set": {
                  "ID": "${ID | default:undefined}",
                  "USERNAME": "${USERNAME | default:undefined}",
                  "DICT_SEX": "${DICT_SEX | default:undefined}",
                  "EMAIL": "${EMAIL | default:undefined}"
                }
              }
            },
            "onEvent": {
              "submitSucc": {
                "actions": [
                  {
                    "actionType": "reload",
                    "componentId": "crud_table"
                  }
                ]
              }
            },
            "body": [
              {
                "type": "uuid",
                "id": "insert-ID",
                "name": "ID"
              },
              {
                "type": "input-text",
                "name": "USERNAME",
                "label": "用户名",
                "maxLength": 50,
                "disabled": false,
                "id": "insert-USERNAME"
              },
              {
                "type": "select",
                "name": "DICT_SEX",
                "label": "性别",
                "maxLength": 100,
                "source": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/sys_dict_items",
                  "data": {
                    "@column": [
                      "item_code",
                      "item_name"
                    ],
                    "@where": [
                      {
                        "column": "dict_code",
                        "condition": "EQ",
                        "value": "sex"
                      }
                    ]
                  },
                  "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name ||  item.ITEM_NAME\n  }))\n};"
                },
                "clearable": true,
                "disabled": false,
                "id": "insert-SEX"
              },
              {
                "type": "input-text",
                "name": "EMAIL",
                "label": "用户邮箱地址",
                "maxLength": 100,
                "disabled": false,
                "id": "insert-EMAIL"
              }
            ]
          }
        }
      },
      "bulkActions",
      {
        "type": "columns-toggler",
        "draggable": true,
        "align": "right"
      },
      {
        "type": "export-excel",
        "label": "导出",
        "icon": "fa fa-file-excel",
        "api": {
          "method": "post",
          "url": "/sql/forge/api/json/select/USERS",
          "data": {
            "@column": [
              "USERS.ID",
              "USERS.USERNAME",
              "sex.item_name as SEX",
              "USERS.EMAIL"
            ],
            "@join": [
              {
                "type": "JOIN",
                "joinTable": "sys_dict_item sex",
                "on": "USERS.DICT_SEX = sex.item_code"
              }
            ],
            "@where": [
              {
                "column": "USERS.USERNAME",
                "condition": "LIKE",
                "value": "${USERNAME | default:undefined}"
              },
              {
                "column": "USERS.DICT_SEX",
                "condition": "IN",
                "value": "${DICT_SEX | default:undefined | split}"
              },
              {
                "column": "USERS.EMAIL",
                "condition": "LIKE",
                "value": "${EMAIL | default:undefined}"
              },
              {
                "column": "sex.dict_code",
                "condition": "EQ",
                "value": "sex"
              }
            ]
          }
        },
        "align": "right"
      }
    ],
    "footerToolbar": [
      "statistics",
      {
        "type": "pagination",
        "layout": "total,perPage,pager,go"
      }
    ],
    "bulkActions": [
      {
        "label": "批量删除",
        "icon": "fa fa-trash",
        "actionType": "ajax",
        "api": {
          "method": "post",
          "url": "/sql/forge/api/json/delete/USERS",
          "data": {
            "@where": [
              {
                "column": "ID",
                "condition": "IN",
                "value": "${ids | split}"
              }
            ]
          }
        },
        "confirmText": "确定要批量删除?"
      }
    ],
    "keepItemSelectionOnPageChange": true,
    "labelTpl": "${USERNAME}",
    "autoFillHeight": true,
    "autoGenerateFilter": true,
    "showIndex": true,
    "primaryField": "ID",
    "columns": [
      {
        "name": "ID",
        "hidden": true
      },
      {
        "name": "USERNAME",
        "label": "用户名",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "USERNAME",
          "label": "用户名",
          "maxLength": 50,
          "placeholder": "输入用户名"
        }
      },
      {
        "name": "SEX",
        "label": "性别",
        "sortable": true,
        "searchable": {
          "type": "select",
          "name": "SEX",
          "label": "性别",
          "maxLength": 100,
          "placeholder": "输入性别",
          "multiple": true,
          "source": {
            "method": "post",
            "url": "/sql/forge/api/json/select/sys_dict_items",
            "data": {
              "@column": [
                "item_code",
                "item_name"
              ],
              "@where": [
                {
                  "column": "dict_code",
                  "condition": "EQ",
                  "value": "sex"
                }
              ]
            },
            "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name ||  item.ITEM_NAME\n  }))\n};"
          },
          "clearable": true
        }
      },
      {
        "name": "EMAIL",
        "label": "用户邮箱地址",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "EMAIL",
          "label": "用户邮箱地址",
          "maxLength": 100,
          "placeholder": "输入用户邮箱地址"
        }
      },
      {
        "type": "operation",
        "label": "操作",
        "buttons": [
          {
            "label": "修改",
            "type": "button",
            "icon": "fa fa-pen-to-square",
            "actionType": "drawer",
            "drawer": {
              "title": "新增表单",
              "body": {
                "type": "form",
                "initApi": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/USERS",
                  "data": {
                    "@column": [
                      "USERS.ID",
                      "USERS.USERNAME",
                      "USERS.SEX",
                      "USERS.EMAIL"
                    ],
                    "@join": [
                      {
                        "type": "JOIN",
                        "joinTable": "sys_dict_item sex_a814d446",
                        "on": "USERS.SEX = sex_a814d446.item_code"
                      }
                    ],
                    "@where": [
                      {
                        "column": "USERS.ID",
                        "condition": "EQ",
                        "value": "${ID}"
                      }
                    ]
                  },
                  "responseData": {
                    "&": "${items | first}"
                  }
                },
                "api": {
                  "method": "post",
                  "url": "/sql/forge/api/json/update/USERS",
                  "data": {
                    "@set": {
                      "ID": "${ID}",
                      "USERNAME": "${USERNAME}",
                      "SEX": "${SEX}",
                      "EMAIL": "${EMAIL}"
                    },
                    "@where": [
                      {
                        "column": "USERS.ID",
                        "condition": "EQ",
                        "value": "${ID}"
                      }
                    ]
                  }
                },
                "body": [
                  {
                    "type": "input-text",
                    "name": "ID",
                    "hidden": true,
                    "id": "update-ID"
                  },
                  {
                    "type": "input-text",
                    "name": "USERNAME",
                    "label": "用户名",
                    "maxLength": 50,
                    "disabled": false,
                    "id": "update-USERNAME"
                  },
                  {
                    "type": "select",
                    "name": "SEX",
                    "label": "性别",
                    "maxLength": 100,
                    "source": {
                      "method": "post",
                      "url": "/sql/forge/api/json/select/sys_dict_item",
                      "data": {
                        "@column": [
                          "item_code",
                          "item_name"
                        ],
                        "@where": [
                          {
                            "column": "dict_code",
                            "condition": "EQ",
                            "value": "sex"
                          }
                        ]
                      },
                      "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name ||  item.ITEM_NAME\n  }))\n};"
                    },
                    "clearable": true,
                    "disabled": false,
                    "id": "update-SEX"
                  },
                  {
                    "type": "input-text",
                    "name": "EMAIL",
                    "label": "用户邮箱地址",
                    "maxLength": 100,
                    "disabled": false,
                    "id": "update-EMAIL"
                  }
                ]
              }
            }
          },
          {
            "label": "删除",
            "type": "button",
            "icon": "fa fa-minus",
            "actionType": "ajax",
            "level": "danger",
            "confirmText": "确认要删除？",
            "api": {
              "method": "post",
              "url": "/sql/forge/api/json/delete/USERS",
              "data": {
                "@where": [
                  {
                    "column": "ID",
                    "condition": "EQ",
                    "value": "${ID}"
                  }
                ]
              }
            }
          }
        ],
        "fixed": "right"
      }
    ]
  }
}
```

# 开始生成
请根据上方提供的【表结构信息】和【API 规范】，生成完整的 Amis CRUD 页面 JSON 配置：