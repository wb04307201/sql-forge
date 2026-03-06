# 表信息
```json
[{
  "table": "USERS",
  "desc": "用户表",
  "type": "crud",
  "fields": {
    "ID": {"type": "uuid", "desc": "用户ID"},
    "USERNAME": {"type": "string", "length": 50, "desc": "用户名","search": true},
    "DICT_SEX": {"type": "dict", "length": 100, "desc": "性别", "dict_code": "sex", "search": true},
    "EMAIL": {"type": "string", "length": 100, "desc": "邮箱地址","search": true}
  }
},
  {
    "table": "SYS_DICT_ITEMS",
    "desc": "字典项表",
    "type": "dict",
    "fields": {
      "DICT_CODE": {"type": "string"},
      "ITEM_CODE": {"type": "string"},
      "ITEM_NAME": {"type": "string"}
    }
  }]
```

# API规范
通用接口，通过`JSON`格式描述数据操作，后端自动生成对应的`SQL`执行并返回结果。

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
  "@column": ["字段名1","别名.字段名2"],
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
- `@column`: 要查询的字段，为空或不传则使用`*`
- `@where`: 查询条件：
  - column: 要匹配的字段名
  - condition: 条件类型（EQ、NOT_EQ、GT、LT、GTEQ、LTEQ、LIKE、NOT_LIKE、LEFT_LIKE、RIGHT_LIKE、BETWEEN、NOT_BETWEEN、IN、NOT_IN、IS_NULL、IS_NOT_NULL）
  - value: 匹配的值
- `@join`: 添加关联表:
  - type: JOIN类型（JOIN, INNER_JOIN, LEFT_OUTER_JOIN, RIGHT_OUTER_JOIN, OUTER_JOIN）
  - joinTable: 关联表名/关联表名 别名
  - on: 关联条件
- `@order`: 排序字段
- `@group`: 分组字段
- `@distince`: 可选参数，是否去重，默认不去重复

###### 示例1
1. 请求
```http request
POST http://localhost:8080/sql/forge/api/json/select/USERS
Content-Type: application/json

{
  "@column": [
    "ID",
    "USERNAME",
    "DICT_SEX",
    "EMAIL"
  ]
}

```

2. 生成的SQL
```sql
SELECT ID, USERNAME, DICT_SEX, EMAIL
FROM USERS
```

###### 示例2
1. 请求
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

2. 生成的SQL
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
- `@column、@where、@join、@order、@distince`: 参见`select`方法对应参数
- `@page`: 分页
  - pageIndex: 页码（从0开始）
  - pageSize: 每页大小

###### 示例
1. 请求
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

2. 生成的SQL
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
- `@with_select`: 可选参数，用于插入后执行一个查询，参见`select`方法

###### 示例
1. 请求
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

2. 生成的SQL
```sql
INSERT INTO users
  (id, username, dict_sex, email)
VALUES (?, ?, ?, ?)
```

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
- `@where`: 参见`select`方法的`@where`
- `@with_select`: 可选参数，用于更新后执行一个查询，参见`select`方法

###### 示例
1. 请求
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

2. 生成的SQL
```sql
UPDATE users
SET email = ?
WHERE (id = ?)
```

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
- `@where`: 参见`select`方法的`@where`
- `@with_select`: 可选参数，用于更新后执行一个查询，参见`select`方法

###### 示例
1. 请求
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

2. 生成的SQL
```sql
DELETE FROM users
WHERE (id = ?)
```

# Amis界面json
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

# 问题
如果我想输入的"表信息",要求大模型生成"用户表"的“crud单表维护”的json配置，并要求:
1. json配置使用百度Amis
2. crud单表维护使用百度Amis的crud组件
3. 按照"API规范"调用通用接口进行数据库操作
我应该使用什么内容的prompt模板，通过输入"表信息"通过大模型生成"Amis界面json"
给我针对“crud单表维护”的可复用prompt模板，并要求：
1. 可复用模板，针对不同的表信息生成对应的"Amis界面json"
2. 按照"API规范"调用通用接口进行数据库操作
3. 模板中包含"API规范"
4. 模板中包含一个示例，示例输入使用当前文档中的"表信息"、示例输出使用当前文档中的"Amis界面json"
5. 模板中输入的"表信息"使用{{TABLE_INFO}}标识，有且只有一个
6. 模板中的"API规范"使用{{API_SPEC}}标识，有且只有一个
7. 模板中的“输入示例”使用{{EXAMPLE_TABLE_INFO}}标识，有且只有一个
8. 模板中的“输出示例"使用{{EXAMPLE_AMIS_INFO}}标识，有且只有一个