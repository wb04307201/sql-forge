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

# 表信息（待处理）
```json
[{
  "table": "PRODUCTS",
  "desc": "商品表",
  "type": "crud",
  "fields": {
    "ID": {"type": "uuid", "desc": "商品ID"},
    "NAME": {"type": "string", "length": 50, "desc": "商品名称","search": true},
    "DICT_CATEGORIES": {"type": "dict", "length": 100, "desc": "商品类型", "dict_code": "categories", "search": true},
    "PRICE": {"type": "number", "max": 9999999999, "precision": 2, "desc": "邮箱地址", "search": true}
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