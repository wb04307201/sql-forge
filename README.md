# SQL Forge - SQL Workshop

<div align="right">
  English | <a href="README.zh-CN.md">中文</a>
</div>

> **SQL Workshop** is not just an ORM framework, but a complete database-based solution that provides:
> - Basic database support
> - Cross-database federated queries
> - JSON-based database CRUD operations
> - Chain operations for entity objects
> - SQL template engine
> - Amis low-code visual editing and template management
> - Web console

[![](https://jitpack.io/v/com.gitee.wb04307201/sql-forge.svg)](https://jitpack.io/#com.gitee.wb04307201/sql-forge)
[![star](https://gitee.com/wb04307201/sql-forge/badge/star.svg?theme=dark)](https://gitee.com/wb04307201/sql-forge)
[![fork](https://gitee.com/wb04307201/sql-forge/badge/fork.svg?theme=dark)](https://gitee.com/wb04307201/sql-forge)
[![star](https://img.shields.io/github/stars/wb04307201/sql-forge)](https://github.com/wb04307201/sql-forge)
[![fork](https://img.shields.io/github/forks/wb04307201/sql-forge)](https://github.com/wb04307201/sql-forge)  
![MIT](https://img.shields.io/badge/License-Apache2.0-blue.svg) ![JDK](https://img.shields.io/badge/JDK-17+-green.svg) ![SpringBoot](https://img.shields.io/badge/Spring%20Boot-3+-green.svg)

## Usage
### Import Dependencies
Add JitPack repository
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

```xml
<dependency>
    <groupId>com.gitee.wb04307201.sql-forge</groupId>
    <artifactId>sql-forge-spring-boot-starter</artifactId>
    <version>1.5.8</version>
</dependency>
<dependency>
    <groupId>jakarta.persistence</groupId>
    <artifactId>jakarta.persistence-api</artifactId>
    <version>3.2.0</version>
</dependency>
```

## Features

### SQL Executor
#### database - Project Database
Directly references the data source already configured in the Spring project
```yaml
sql:
  forge:
    schemata:  # Configure schema
      - PUBLIC
```
#### calcite - Apache Calcite Cross-Database Federated Query
Enable calcite and specify Apache Calcite data source information
```yaml
sql:
  forge:
    calcite:
      enabled: true
      configuration: classpath:model.json
      schemata: # Configure schema
        - MYSQL
        - POSTGRES
```
#### Custom Extension
Extend [IExecutor.java](sql-forge-core/src/main/java/cn/wubo/sql/forge/IExecutor.java) to implement custom executors

### Json API Module
Allows frontend to operate the database without writing backend code. Describes the required data structure and operations through `JSON` format, and the backend automatically generates corresponding `SQL` execution and returns results.

- **Request Path**: `sql/forge/api/json/{method}/{tableName}?executorName={executorName}`
- **Request Method**: `POST`
- **Content Type**: `application/json`
- **Path Parameters**:
  - `{method}`: Operation method type (delete, insert, select, update)
  - `{tableName}`: Database table name
  - `{executorName}`: Database executor name, defaults to database (project database), calcite (Apache Calcite cross-database federated query), supports custom extensions. If not provided, defaults to database

#### delete Method

#### Request Format
```json
{
  "@where": [
    {
      "column": "column_name",
      "condition": "condition_type",
      "value": "value"
    }
  ],
  "@with_select": {
    // Query JSON after deletion
  }
}
```

#### Parameter Description
- `@where`: Delete condition array, each condition contains:
  - column: Field name to match
  - condition: Condition type (EQ, NOT_EQ, GT, LT, GTEQ, LTEQ, LIKE, NOT_LIKE, LEFT_LIKE, RIGHT_LIKE, BETWEEN, NOT_BETWEEN, IN, NOT_IN, IS_NULL, IS_NOT_NULL)
  - value: Value to match
- `@with_select`: Optional query condition for executing a query after deletion

#### insert Method

#### Request Format
```json
{
  "@set": {
    "field1": "value1",
    "field2": "value2"
  },
  "@with_select": {
    // Query JSON after deletion
  }
}
```

#### Parameter Description
- `@set`: Key-value pairs of fields and values to insert, at least one field required
- `@with_select`: Optional query condition for executing a query after insertion

#### select Method

#### Request Format
```json
{
  "@column": ["field1", "field2"],
  "@where": [
    {
      "column": "field_name",
      "condition": "condition_type",
      "value": "value"
    }
  ],
  "@join": [
    {
      "type": "JOIN_type",
      "joinTable": "join_table_name",
      "on": "join_condition"
    }
  ],
  "@order": ["field_name ASC", "field_name DESC"],
  "@group": ["field_name"],
  "@distince": false
}
```

##### Parameter Description
- `@column`: Array of fields to query, if empty queries all fields
- `@where`: Query condition array
- `@join`: Join query condition array
- `@order`: Sort field array
- `@group`: Group by field array
- `@distince`: Whether to deduplicate

#### selectPage Method

#### Request Format
```json
{
  "@column": ["field1", "field2"],
  "@where": [
    {
      "column": "field_name",
      "condition": "condition_type",
      "value": "value"
    }
  ],
  "@page": {
    "pageIndex": 0,
    "pageSize": 10
  },
  "@join": [
    {
      "type": "JOIN_type",
      "joinTable": "join_table_name",
      "on": "join_condition"
    }
  ],
  "@order": ["field_name ASC", "field_name DESC"],
  "@distince": false
}
```

##### Parameter Description
- `@column`: Array of fields to query, if empty queries all fields
- `@where`: Query condition array
- `@page` Pagination parameters
  - pageIndex: Page number (starting from 0)
  - pageSize: Page size
- `@join`: Join query condition array
- `@order`: Sort field array
- `@distince`: Whether to deduplicate

#### update Method

##### Request Format
```json
{
  "@set": {
    "field1": "new_value1",
    "field2": "new_value2"
  },
  "@where": [
    {
      "column": "field_name",
      "condition": "condition_type",
      "value": "value"
    }
  ],
  "@with_select": {
    // Query JSON after deletion
  }
}
```

##### Parameter Description
- `@set`: Key-value pairs of fields and new values to update, at least one field required
- `@where`: Update condition array, specifies which records to update
- `@with_select`: Optional query condition for executing a query after update

#### Examples
1. Query
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

2. Paginated Query
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

3. Insert
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

4. Update
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
5. Delete
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

#### Before Method Execution Aspect
You can customize JSON adjustments before method execution by implementing [IBeforeRecordExecutor.java](sql-forge-record/src/main/java/cn/wubo/sql/forge/record/IBeforeRecordExecutor.java) interface to achieve password encryption, automatic timestamp updates, access control, logging, auditing, etc.

For example, implementing logging on Insert:
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

#### Configuration
Can be disabled via `sql.forge.api.json.enabled=false`

### SQL Template API Module
Provides `SQL` template engine functionality, supporting template syntax such as conditional judgments and loops, dynamically generates `SQL` execution based on parameters and returns results.
- **API Template Management**: Provides management functions for API templates including storage, query, deletion, etc.
- **Templated API Execution**: Supports executing predefined API templates through template ID and parameters

#### Template Management Interfaces

- `PUT /sql/forge/api/template/sql` - Save/Update SQL Template
  - id: Template ID
  - executorName: Executor name, defaults to database (project database), calcite (Apache Calcite cross-database federated query), supports custom extensions
  - context: Template content
- `GET /sql/forge/api/template/sql/{id}` - Get SQL Template by ID
- `GET /sql/forge/api/template/sql` - Get SQL Template List
- `DELETE /sql/forge/api/template/{id}` - Delete SQL Template by ID
- `POST /sql/forge/api/template/sql/{id}` - Execute SQL Template by ID
  - Template parameters Map

#### Example
Template configuration:
```http request
PUT http://localhost:8080/sql/forge/api/template/sql
content-type: application/json

{
    "id": "sql-template-database",
    "type": "templateSql",
    "executorName": "database",
    "context": "SELECT * FROM users WHERE 1=1\r\n<if test=\"name != null && name != ''\">AND username = #{name}</if>\r\n<if test=\"ids != null && !ids.isEmpty()\"><foreach collection=\"ids\" item=\"id\" open=\"AND id IN (\" separator=\",\" close=\")\">#{id}</foreach></if>\r\n<if test=\"(name == null || name == '') && (ids == null || ids.isEmpty()) \">AND 0=1</if>\r\nORDER BY username DESC"
}
```

Execute template:
```http request
POST http://localhost:8080/sql/forge/api/template/sql-template-database
content-type: application/json

{
"name":"alice",
"ids":null
}
```

Response:
```json
[
  {
    "ID": "1",
    "USERNAME": "alice",
    "DICT_SEX": "female",
    "EMAIL": "alice@example.com"
  }
]
```

#### Configuration
Can be disabled via `sql.forge.api.template.sql.enabled=false`

#### Persistent Template
Extend [ITemplateSqlStorage.java](sql-forge-template/src/main/java/cn/wubo/sql/forge/ITemplateSqlStorage.java) to implement your own template service

### Amis Template API Module
Use [Amis](https://aisuda.bce.baidu.com/amis/zh-CN/docs/index) together with **Json API** module, **Template API** module, and **Calcite API** module to quickly build web pages.

#### Template Management Interfaces

- `PUT /sql/forge/api/template/amis` - Save New API Template
  - id: Template ID
  - context: Template content
- `GET /sql/forge/api/template/amis/{id}` - Get Template by ID
- `GET /sql/forge/api/template/amis` - Get Template List
- `DELETE /sql/forge/api/template/amis{id}` - Delete Template by ID

#### Example
Template configuration:
```http request
PUT http://localhost:8080/sql/forge/amis/template
content-type: application/json

{
    "id": "amis-template-users",
    "context": "{\r\n  \"type\": \"page\",\r\n  \"body\": {\r\n    \"type\": \"crud\",\r\n    \"id\": \"crud_table\",\r\n    \"api\": {\r\n      \"method\": \"post\",\r\n      \"url\": \"/sql/forge/api/json/selectPage/USERS\",\r\n      \"data\": {\r\n        \"@column\": [\r\n          \"USERS.ID\",\r\n          \"USERS.USERNAME\",\r\n          \"sex.item_name as SEX\",\r\n          \"USERS.EMAIL\"\r\n        ],\r\n        \"@join\": [\r\n          {\r\n            \"type\": \"LEFT_OUTER_JOIN\",\r\n            \"joinTable\": \"sys_dict_items sex\",\r\n            \"on\": \"USERS.DICT_SEX = sex.item_code\"\r\n          }\r\n        ],\r\n        \"@where\": [\r\n          {\r\n            \"column\": \"USERS.USERNAME\",\r\n            \"condition\": \"LIKE\",\r\n            \"value\": \"${USERNAME | default:undefined}\"\r\n          },\r\n          {\r\n            \"column\": \"USERS.DICT_SEX\",\r\n            \"condition\": \"IN\",\r\n            \"value\": \"${SEX | default:undefined | split}\"\r\n          },\r\n          {\r\n            \"column\": \"USERS.EMAIL\",\r\n            \"condition\": \"LIKE\",\r\n            \"value\": \"${EMAIL | default:undefined}\"\r\n          },\r\n          {\r\n            \"column\": \"sex.dict_code\",\r\n            \"condition\": \"EQ\",\r\n            \"value\": \"sex\"\r\n          }\r\n        ],\r\n        \"@order\": [\r\n          \"${default(orderBy && orderDir ? (orderBy + ' ' + orderDir):'',undefined)}\"\r\n        ],\r\n        \"@page\": {\r\n          \"pageIndex\": \"${page - 1}\",\r\n          \"pageSize\": \"${perPage}\"\r\n        }\r\n      }\r\n    },\r\n    \"headerToolbar\": [\r\n      {\r\n        \"label\": \"Add\",\r\n        \"type\": \"button\",\r\n        \"icon\": \"fa fa-plus\",\r\n        \"level\": \"primary\",\r\n        \"actionType\": \"drawer\",\r\n        \"drawer\": {\r\n          \"title\": \"Add Form\",\r\n          \"body\": {\r\n            \"type\": \"form\",\r\n            \"api\": {\r\n              \"method\": \"post\",\r              \"url\": \"/sql/forge/api/json/insert/USERS\",\r\n              \"data\": {\r\n                \"@set\": {\r\n                  \"ID\": \"${ID | default:undefined}\",\r\n                  \"USERNAME\": \"${USERNAME | default:undefined}\",\r\n                  \"DICT_SEX\": \"${DICT_SEX | default:undefined}\",\r\n                  \"EMAIL\": \"${EMAIL | default:undefined}\"\r\n                }\r\n              }\r\n            },\r\n            \"onEvent\": {\r\n              \"submitSucc\": {\r\n                \"actions\": [\r\n                  {\r\n                    \"actionType\": \"reload\",\r\n                    \"componentId\": \"crud_table\"\r\n                  }\r\n                ]\r\n              }\r\n            },\r\n            \"body\": [\r\n              {\r\n                \"type\": \"uuid\",\r\n                \"id\": \"insert-ID\",\r\n                \"name\": \"ID\"\r\n              },\r\n              {\r\n                \"type\": \"input-text\",\r\n                \"name\": \"USERNAME\",\r\n                \"label\": \"Username\",\r\n                \"maxLength\": 50,\r\n                \"disabled\": false,\r\n                \"id\": \"insert-USERNAME\"\r\n              },\r\n              {\r\n                \"type\": \"select\",\r\n                \"name\": \"DICT_SEX\",\r\n                \"label\": \"Gender\",\r\n                \"maxLength\": 100,\r\n                \"source\": {\r\n                  \"method\": \"post\",\r\n                  \"url\": \"/sql/forge/api/json/select/sys_dict_items\",\r\n                  \"data\": {\r\n                    \"@column\": [\r\n                      \"item_code\",\r\n                      \"item_name\"\r\n                    ],\r\n                    \"@where\": [\r\n                      {\r\n                        \"column\": \"dict_code\",\r\n                        \"condition\": \"EQ\",\r\n                        \"value\": \"sex\"\r\n                      }\r\n                    ]\r\n                  },\r\n                  \"adaptor\": \"return {\\n  options: payload.map(item => ({\\n    value: item.item_code || item.ITEM_CODE,\\n    label: item.item_name ||  item.ITEM_NAME\\n  }))\\n};\"\r\n                },\r\n                \"clearable\": true,\r\n                \"disabled\": false,\r\n                \"id\": \"insert-SEX\"\r\n              },\r\n              {\r\n                \"type\": \"input-text\",\r\n                \"name\": \"EMAIL\",\r\n                \"label\": \"User Email Address\",\r\n                \"maxLength\": 100,\r\n                \"disabled\": false,\r\n                \"id\": \"insert-EMAIL\"\r\n              }\r\n            ]\r\n          }\r\n        }\r\n      },\r\n      \"bulkActions\",\r\n      {\r\n        \"type\": \"columns-toggler\",\r\n        \"draggable\": true,\r\n        \"align\": \"right\"\r\n      },\r\n      {\r\n        \"type\": \"export-excel\",\r\n        \"label\": \"Export\",\r\n        \"icon\": \"fa fa-file-excel\",\r\n        \"api\": {\r\n          \"method\": \"post\",\r\n          \"url\": \"/sql/forge/api/json/select/USERS\",\r\n          \"data\": {\r\n            \"@column\": [\r\n              \"USERS.ID\",\r\n              \"USERS.USERNAME\",\r\n              \"sex.item_name as SEX\",\r\n              \"USERS.EMAIL\"\r\n            ],\r\n            \"@join\": [\r\n              {\r\n                \"type\": \"LEFT_OUTER_JOIN\",\r\n                \"joinTable\": \"sys_dict_item sex\",\r\n                \"on\": \"USERS.DICT_SEX = sex.item_code\"\r\n              }\r\n            ],\r\n            \"@where\": [\r\n              {\r\n                \"column\": \"USERS.USERNAME\",\r\n                \"condition\": \"LIKE\",\r\n                \"value\": \"${USERNAME | default:undefined}\"\r\n              },\r\n              {\r\n                \"column\": \"USERS.DICT_SEX\",\r\n                \"condition\": \"IN\",\r\n                \"value\": \"${DICT_SEX | default:undefined | split}\"\r\n              },\r\n              {\r\n                \"column\": \"USERS.EMAIL\",\r\n                \"condition\": \"LIKE\",\r\n                \"value\": \"${EMAIL | default:undefined}\"\r\n              },\r\n              {\r\n                \"column\": \"sex.dict_code\",\r\n                \"condition\": \"EQ\",\r\n                \"value\": \"sex\"\r\n              }\r\n            ]\r\n          }\r\n        },\r\n        \"align\": \"right\"\r\n      }\r\n    ],\r\n    \"footerToolbar\": [\r\n      \"statistics\",\r\n      {\r\n        \"type\": \"pagination\",\r\n        \"layout\": \"total,perPage,pager,go\"\r\n      }\r\n    ],\r\n    \"bulkActions\": [\r\n      {\r\n        \"label\": \"Batch Delete\",\r\n        \"icon\": \"fa fa-trash\",\r\n        \"actionType\": \"ajax\",\r\n        \"api\": {\r\n          \"method\": \"post\",\r\n          \"url\": \"/sql/forge/api/json/delete/USERS\",\r\n          \"data\": {\r\n            \"@where\": [\r\n              {\r\n                \"column\": \"ID\",\r\n                \"condition\": \"IN\",\r\n                \"value\": \"${ids | split}\"\r\n              }\r\n            ]\r\n          }\r\n        },\r\n        \"confirmText\": \"Are you sure you want to batch delete?\"\r\n      }\r\n    ],\r\n    \"keepItemSelectionOnPageChange\": true,\r\n    \"labelTpl\": \"${USERNAME}\",\r\n    \"autoFillHeight\": true,\r\n    \"autoGenerateFilter\": true,\r\n    \"showIndex\": true,\r\n    \"primaryField\": \"ID\",\r\n    \"columns\": [\r\n      {\r\n        \"name\": \"ID\",\r\n        \"hidden\": true\r\n      },\r\n      {\r\n        \"name\": \"USERNAME\",\r\n        \"label\": \"Username\",\r\n        \"sortable\": true,\r\n        \"searchable\": {\r\n          \"type\": \"input-text\",\r\n          \"name\": \"USERNAME\",\r\n          \"label\": \"Username\",\r\n          \"maxLength\": 50,\r\n          \"placeholder\": \"Enter username\"\r\n        }\r\n      },\r\n      {\r\n        \"name\": \"SEX\",\r\n        \"label\": \"Gender\",\r\n        \"sortable\": true,\r\n        \"searchable\": {\r\n          \"type\": \"select\",\r\n          \"name\": \"SEX\",\r\n          \"label\": \"Gender\",\r\n          \"maxLength\": 100,\r\n          \"placeholder\": \"Enter gender\",\r\n          \"multiple\": true,\r\n          \"source\": {\r\n            \"method\": \"post\",\r\n            \"url\": \"/sql/forge/api/json/select/sys_dict_items\",\r\n            \"data\": {\r\n              \"@column\": [\r\n                \"item_code\",\r\n                \"item_name\"\r\n              ],\r\n              \"@where\": [\r\n                {\r\n                  \"column\": \"dict_code\",\r\n                  \"condition\": \"EQ\",\r\n                  \"value\": \"sex\"\r\n                }\r\n              ]\r\n            },\r\n            \"adaptor\": \"return {\\n  options: payload.map(item => ({\\n    value: item.item_code || item.ITEM_CODE,\\n    label: item.item_name ||  item.ITEM_NAME\\n  }))\\n};\"\r\n          },\r\n          \"clearable\": true\r\n        }\r\n      },\r\n      {\r\n        \"name\": \"EMAIL\",\r\n        \"label\": \"User Email Address\",\r\n        \"sortable\": true,\r\n        \"searchable\": {\r\n          \"type\": \"input-text\",\r\n          \"name\": \"EMAIL\",\r\n          \"label\": \"User Email Address\",\r\n          \"maxLength\": 100,\r\n          \"placeholder\": \"Enter user email address\"\r\n        }\r\n      },\r\n      {\r\n        \"type\": \"operation\",\r\n        \"label\": \"Operations\",\r\n        \"buttons\": [\r\n          {\r\n            \"label\": \"Edit\",\r\n            \"type\": \"button\",\r\n            \"icon\": \"fa fa-pen-to-square\",\r\n            \"actionType\": \"drawer\",\r\n            \"drawer\": {\r\n              \"title\": \"Edit Form\",\r\n              \"body\": {\r\n                \"type\": \"form\",\r\n                \"initApi\": {\r\n                  \"method\": \"post\",\r\n                  \"url\": \"/sql/forge/api/json/select/USERS\",\r\n                  \"data\": {\r\n                    \"@column\": [\r\n                      \"USERS.ID\",\r\n                      \"USERS.USERNAME\",\r\n                      \"USERS.SEX\",\r\n                      \"USERS.EMAIL\"\r\n                    ],\r\n                    \"@join\": [\r\n                      {\r\n                        \"type\": \"LEFT_OUTER_JOIN\",\r\n                        \"joinTable\": \"sys_dict_item sex_a814d446\",\r\n                        \"on\": \"USERS.SEX = sex_a814d446.item_code\"\r\n                      }\r\n                    ],\r\n                    \"@where\": [\r\n                      {\r\n                        \"column\": \"USERS.ID\",\r\n                        \"condition\": \"EQ\",\r\n                        \"value\": \"${ID}\"\r\n                      }\r\n                    ]\r\n                  },\r\n                  \"responseData\": {\r\n                    \"&\": \"${items | first}\"\r\n                  }\r\n                },\r\n                \"api\": {\r\n                  \"method\": \"post\",\r\n                  \"url\": \"/sql/forge/api/json/update/USERS\",\r\n                  \"data\": {\r\n                    \"@set\": {\r\n                      \"ID\": \"${ID}\",\r\n                      \"USERNAME\": \"${USERNAME}\",\r\n                      \"SEX\": \"${SEX}\",\r\n                      \"EMAIL\": \"${EMAIL}\"\r\n                    },\r\n                    \"@where\": [\r\n                      {\r\n                        \"column\": \"USERS.ID\",\r\n                        \"condition\": \"EQ\",\r\n                        \"value\": \"${ID}\"\r\n                      }\r\n                    ]\r\n                  }\r\n                },\r\n                \"body\": [\r\n                  {\r\n                    \"type\": \"input-text\",\r\n                    \"name\": \"ID\",\r\n                    \"hidden\": true,\r\n                    \"id\": \"update-ID\"\r\n                  },\r\n                  {\r\n                    \"type\": \"input-text\",\r\n                    \"name\": \"USERNAME\",\r\n                    \"label\": \"Username\",\r\n                    \"maxLength\": 50,\r\n                    \"disabled\": false,\r\n                    \"id\": \"update-USERNAME\"\r\n                  },\r\n                  {\r\n                    \"type\": \"select\",\r\n                    \"name\": \"SEX\",\r\n                    \"label\": \"Gender\",\r\n                    \"maxLength\": 100,\r\n                    \"source\": {\r\n                      \"method\": \"post\",\r\n                      \"url\": \"/sql/forge/api/json/select/sys_dict_item\",\r\n                      \"data\": {\r\n                        \"@column\": [\r\n                          \"item_code\",\r\n                          \"item_name\"\r\n                        ],\r\n                        \"@where\": [\r\n                          {\r\n                            \"column\": \"dict_code\",\r\n                            \"condition\": \"EQ\",\r\n                            \"value\": \"sex\"\r\n                          }\r\n                        ]\r\n                      },\r\n                      \"adaptor\": \"return {\\n  options: payload.map(item => ({\\n    value: item.item_code || item.ITEM_CODE,\\n    label: item.item_name ||  item.ITEM_NAME\\n  }))\\n};\"\r\n                    },\r\n                    \"clearable\": true,\r\n                    \"disabled\": false,\r\n                    \"id\": \"update-SEX\"\r\n                  },\r\n                  {\r\n                    \"type\": \"input-text\",\r\n                    \"name\": \"EMAIL\",\r\n                    \"label\": \"User Email Address\",\r\n                    \"maxLength\": 100,\r\n                    \"disabled\": false,\r\n                    \"id\": \"update-EMAIL\"\r\n                  }\r\n                ]\r\n              }\r\n            }\r\n          },\r\n          {\r\n            \"label\": \"Delete\",\r\n            \"type\": \"button\",\r\n            \"icon\": \"fa fa-minus\",\r\n            \"actionType\": \"ajax\",\r\n            \"level\": \"danger\",\r\n            \"confirmText\": \"Are you sure you want to delete?\",\r\n            \"api\": {\r\n              \"method\": \"post\",\r\n              \"url\": \"/sql/forge/api/json/delete/USERS\",\r\n              \"data\": {\r\n                \"@where\": [\r\n                  {\r\n                    \"column\": \"ID\",\r\n                    \"condition\": \"EQ\",\r\n                    \"value\": \"${ID}\"\r\n                  }\r\n                ]\r\n              }\r\n            }\r\n          }\r\n        ],\r\n        \"fixed\": \"right\"\r\n      }\r\n    ]\r\n  }\r\n}"
}
```

Rendered page:
![img.png](img.png)

#### Configuration
Can be disabled via `sql.forge.api.template.amis.enabled=false`

#### Persistent Template
Extend [ITemplateAmisStorage.java](sql-forge-template/src/main/java/cn/wubo/sql/forge/ITemplateAmisStorage.java) to implement your own template service

### Entity Module
- [Entity.java](sql-forge-entity/src/main/java/cn/eubo/sql/forge/Entity.java) Provides builders for database operations on entity objects, including delete, insert, query, update, save operations, simplifying the `SQL` building process.
- [EntityExecutor.java](sql-forge-entity/src/main/java/cn/eubo/sql/forge/EntityExecutor.java) Responsible for executing database operations of the **builder**.

#### Features
- Uses chain calls
- Supports type-safe generic operations
- Flexibly configures query conditions through builder pattern
- Unified database operation entry point

#### Usage Examples

Assuming there is a user entity class [User](sql-forge-test/src/test/java/cn/wubo/sql/forge/User.java):

```java
@Autowired
private EntityService entityService;


// Query operation
EntitySelect<User> select = Entity.select(User.class)
                .distinct(true)
                .columns(User::getId, User::getUsername, User::getEmail)
                .orders(User::getUsername)
                .in(User::getUsername, "alice", "bob");
List<User> users = entityService.run(select);
Object key = entityService.run(insert);

// Paginated query operation
EntitySelectPage<User> select = Entity.selectPage(User.class)
        .distinct(true)
        .columns(User::getId, User::getUsername, User::getEmail)
        .orders(User::getUsername)
        .in(User::getUsername, "alice", "bob")
        .page(0, 1);
SelectPageResult<User> users = entityService.run(select);

// Insert operation  
EntityInsert<User> insert = Entity.insert(User.class).set(User::getId, id)
        .set(User::getUsername, "wb04307201")
        .set(User::getEmail, "wb04307201@gitee.com");
int count = entityService.run(update);

// Update operation
EntityUpdate<User> update = Entity.update(User.class)
        .set(User::getEmail, "wb04307201@github.com")
        .eq(User::getId, id);
int count = entityService.run(update);

// Delete operation
EntityDelete<User> delete = Entity.delete(User.class)
        .eq(User::getId, id);
count = entityService.run(delete);

// Object save operation (insert or update)
User user = new User();
user.setUsername("wb04307201");
user.setEmail("wb04307201@gitee.com");
user = entityService.run(Entity.save(user));
user.setEmail("wb04307201@github.com");
user = entityService.run(Entity.save(user));

// Object delete operation
int count = entityService.run(Entity.delete(user));
```

#### Query Builder Description

##### 1. Column Selection
- column(SFunction<T, ?> column) - Select single column
- columns(SFunction<T, ?>... columns) - Select multiple columns

##### 2. Query Conditions
- eq(SFunction<T, ?> column, Object value) - Equals
- neq(SFunction<T, ?> column, Object value) - Not equals
- gt(SFunction<T, ?> column, Object value) - Greater than
- lt(SFunction<T, ?> column, Object value) - Less than
- gteq(SFunction<T, ?> column, Object value) - Greater than or equals
- lteq(SFunction<T, ?> column, Object value) - Less than or equals
- like(SFunction<T, ?> column, Object value) - Like
- notLike(SFunction<T, ?> column, Object value) - Not like
- leftLike(SFunction<T, ?> column, Object value) - Left like
- rightLike(SFunction<T, ?> column, Object value) - Right like
- between(SFunction<T, ?> column, Object value1, Object value2) - Between
- notBetween(SFunction<T, ?> column, Object value1, Object value2) - Not between
- in(SFunction<T, ?> column, Object... value) - In
- notIn(SFunction<T, ?> column, Object... value) - Not in
- isNull(SFunction<T, ?> column) - Is NULL
- isNotNull(SFunction<T, ?> column) - Is NOT NULL

##### 3. Sorting
- orderAsc(SFunction<T, ?> column) - Ascending sort
- orderDesc(SFunction<T, ?> column) - Descending sort
- orders(SFunction<T, ?>... columns) - Multi-column sorting (default ascending)

##### 4. Pagination
- page(Integer pageIndex, Integer pageSize) - Set pagination parameters

##### 5. Deduplication
- distinct(Boolean distinct) - Set whether to deduplicate

#### Object Save Operation Builder Description

Determines primary key field based on `@Id` annotation. If no primary key field exists, throws `IllegalArgumentException`
- **Insert condition**: Executes insert operation when primary key value is `null`
  - `String` type primary key: Automatically generates `UUID` as primary key value
  - Other type primary keys: Uses database auto-generated primary key value
- **Update condition**: Executes update operation when primary key value is not `null`
  - Uses primary key value as update condition

### Console
Provides a simple web interface for debugging and template management:
- Database metadata viewing, SQL debugging (disabled by default, enable via configuration `sql.forge.api.database.enabled=true`, only supports queries by default, allow other operations via `sql.forge.api.database.select-only=false`)
  ![img_1.png](img_1.png)
- Json API debugging
  ![img_2.png](img_2.png)
- SQL Template API template maintenance, debugging
  ![img_3.png](img_3.png)
- Amis Template API template maintenance, debugging
  ![img_4.png](img_4.png)

#### Configuration
Can be disabled via `sql.forge.console.enabled=false`
