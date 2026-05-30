# SQL Forge - SQL工坊

<div align="right">
  <a href="README.md">English</a> | 中文
</div>

> **SQL工坊** — 一套基于 Spring Boot 的数据库操作框架，提供 JSON CRUD API、类型安全的实体操作、SQL 模板引擎、Apache Calcite 跨库联邦查询、Amis 低代码可视化管理以及 MCP（Model Context Protocol）AI 工具集成服务器，开箱即用、按需引入。

![Maven Central](https://img.shields.io/maven-central/v/io.github.wb04307201/sql-forge-spring-boot-starter?style=flat-square)
[![star](https://gitee.com/wb04307201/sql-forge/badge/star.svg?theme=dark)](https://gitee.com/wb04307201/sql-forge)
[![fork](https://gitee.com/wb04307201/sql-forge/badge/fork.svg?theme=dark)](https://gitee.com/wb04307201/sql-forge)
[![star](https://img.shields.io/github/stars/wb04307201/sql-forge)](https://github.com/wb04307201/sql-forge)
[![fork](https://img.shields.io/github/forks/wb04307201/sql-forge)](https://github.com/wb04307201/sql-forge)  
![Apache-2.0](https://img.shields.io/badge/License-Apache2.0-blue.svg) ![JDK](https://img.shields.io/badge/JDK-17+-green.svg) ![SpringBoot](https://img.shields.io/badge/Spring%20Boot-3+-green.svg)

## 快速开始

### 引入依赖

根据需求引入对应 Starter：

```xml
<!-- 基础数据库操作（必选） -->
<dependency>
    <groupId>io.github.wb04307201</groupId>
    <artifactId>sql-forge-spring-boot-starter</artifactId>
    <version>1.5.11</version>
</dependency>

<!-- Calcite 跨库联邦查询（可选） -->
<dependency>
    <groupId>io.github.wb04307201</groupId>
    <artifactId>sql-forge-calcite-spring-boot-starter</artifactId>
    <version>1.5.11</version>
</dependency>

<!-- Amis 模板 + Web Console（可选） -->
<dependency>
    <groupId>io.github.wb04307201</groupId>
    <artifactId>sql-forge-web-spring-boot-starter</artifactId>
    <version>1.5.11</version>
</dependency>
```

使用 Entity 模块的 `@Id`、`@Table`、`@Column` 等注解时需额外引入：

```xml
<dependency>
    <groupId>jakarta.persistence</groupId>
    <artifactId>jakarta.persistence-api</artifactId>
    <version>3.2.0</version>
</dependency>
```

## Starter 一览

| Starter | 说明 |
|---------|------|
| **sql-forge-spring-boot-starter** | 基础 Starter：数据库执行器、JSON CRUD API、类型安全实体操作（Entity）、SQL 模板引擎、Record 操作切面、内置用户认证（Session + ApiKey） |
| **sql-forge-calcite-spring-boot-starter** | 基于 Apache Calcite 的跨数据库联邦查询执行器，可同时查询 MySQL、PostgreSQL 等多种数据源 |
| **sql-forge-web-spring-boot-starter** | Amis 低代码模板管理 API、Web Console 可视化界面、用户/角色管理 |
| **sql-forge-mcp** | Model Context Protocol (MCP) 服务器，用于 AI 工具集成 — 通过 stdio 暴露数据库元数据查询、SQL 执行、Amis 模板管理等 MCP 工具 |

---

## 1. sql-forge-spring-boot-starter（基础 Starter）

提供数据库操作的核心能力，包括数据库执行器管理、JSON CRUD API、Entity 链式操作、SQL 模板引擎和 Record 切面扩展。

### 1.1 SQL 执行器

`sql-forge-spring-boot-starter` 自动注册一个名为 `database` 的执行器，直接使用 Spring 项目中已配置的数据源。

```yaml
sql:
  forge:
    schemata:  # 配置 schema
      - PUBLIC
```

#### 自定义执行器

实现 [IExecutor](sql-forge-core/src/main/java/cn/wubo/sql/forge/IExecutor.java) 接口并注册为 Spring Bean 即可扩展自定义执行器。

```java
@Component
public class MyCustomExecutor implements IExecutor {
    @Override
    public String getExecutorName() {
        return "myCustom";
    }
    // ... 实现其他方法
}
```

### 1.2 数据库直连 API

提供直接执行 SQL 的能力（默认关闭，需手动开启）。

```yaml
sql:
  forge:
    api:
      database:
        enabled: true       # 开启数据库直连 API
        select-only: true   # true=仅允许 SELECT，false=允许所有操作
```

- `POST /sql/forge/api/database/execute?executorName=database` - 执行 SQL

### 1.3 JSON CRUD API

让前端无需编写后端代码即可操作数据库 — 通过 `JSON` 格式描述需要的数据结构和操作，后端自动生成对应的 `SQL` 执行并返回结果。

- **请求路径**: `sql/forge/api/json/{method}/{tableName}?executorName={executorName}`
- **请求方法**: `POST`
- **内容类型**: `application/json`
- **路径参数**:
  - `{method}`: 操作方法类型（delete、insert、select、selectPage、update）
  - `{tableName}`: 数据库表名称
  - `{executorName}`: 执行器名称，不传则默认使用 `database`

#### delete 方法

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
    // 删除后查询 JSON
  }
}
```

**参数说明**
- `@where`: 删除条件数组
  - column: 要匹配的字段名
  - condition: 条件类型（EQ、NOT_EQ、GT、LT、GTEQ、LTEQ、LIKE、NOT_LIKE、LEFT_LIKE、RIGHT_LIKE、BETWEEN、NOT_BETWEEN、IN、NOT_IN、IS_NULL、IS_NOT_NULL）
  - value: 匹配的值
- `@with_select`: 可选，删除后执行一个查询

#### insert 方法

```json
{
  "@set": {
    "字段名1": "值1",
    "字段名2": "值2"
  },
  "@with_select": {
    // 插入后查询 JSON
  }
}
```

**参数说明**
- `@set`: 要插入的字段和值的键值对，至少一个字段
- `@with_select`: 可选，插入后执行一个查询

#### select 方法

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

**参数说明**
- `@column`: 要查询的字段数组，为空则查询所有字段
- `@where`: 查询条件数组
- `@join`: 关联查询条件数组
- `@order`: 排序字段数组
- `@group`: 分组字段数组
- `@distince`: 是否去重

#### selectPage 方法

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

**参数说明**
- `@column`: 要查询的字段数组，为空则查询所有字段
- `@where`: 查询条件数组
- `@page`: 分页参数（pageIndex 页码从 0 开始，pageSize 每页大小）
- `@join`: 关联查询条件数组
- `@order`: 排序字段数组
- `@distince`: 是否去重

#### update 方法

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
    // 更新后查询 JSON
  }
}
```

**参数说明**
- `@set`: 要更新的字段和新值的键值对，至少一个字段
- `@where`: 更新条件数组
- `@with_select`: 可选，更新后执行一个查询

#### 示例

1. 查询

```http request
POST http://localhost:8080/sql/forge/api/json/select/orders o
Content-Type: application/json

{
  "@column": [
    "u.username",
    "o.total_amount",
    "p.name               AS product_name",
    "oi.unit_price",
    "oi.quantity",
    "p.price"
  ],
  "@where": [
    {
      "column": "u.username",
      "condition": "IS_NOT_NULL",
      "value": null
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
      "joinTable": "order_items oi",
      "on": "o.id = oi.order_id"
    },
    {
      "type": "JOIN",
      "joinTable": "products p",
      "on": "oi.product_id = p.id"
    }
  ],
  "@order": [
    "o.order_date"
  ]
}
```

2. 分页查询

在查询 JSON 基础上增加 `@page` 参数即可：

```http request
POST http://localhost:8080/sql/forge/api/json/selectPage/orders o
Content-Type: application/json

{
  "@column": ["o.total_amount", "p.name AS product_name"],
  "@join": [
    {
      "type": "JOIN",
      "joinTable": "products p",
      "on": "o.product_id = p.id"
    }
  ],
  "@order": ["o.order_date"],
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
    "password": "123456",
    "enabled": true,
    "category": "user"
  },
  "@with_select": {
    "@where": [
      {
        "column": "id",
        "condition": "EQ",
        "value": "26a05ba3-913d-4085-a505-36d40021c8d1"
      }
    ]
  }
}
```

4. 更新

```http request
POST http://localhost:8080/sql/forge/api/json/update/users
Content-Type: application/json

{
  "@set": {
    "password": "newpassword"
  },
  "@where": [
    {
      "column": "id",
      "condition": "EQ",
      "value": "26a05ba3-913d-4085-a505-36d40021c8d1"
    }
  ],
  "@with_select": {
    "@where": [
      {
        "column": "id",
        "condition": "EQ",
        "value": "26a05ba3-913d-4085-a505-36d40021c8d1"
      }
    ]
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
    "@where": [
      {
        "column": "id",
        "condition": "EQ",
        "value": "26a05ba3-913d-4085-a505-36d40021c8d1"
      }
    ]
  }
}
```

#### 方法执行前切面

通过实现 [IBeforeRecordExecutor](sql-forge-record/src/main/java/cn/wubo/sql/forge/record/IBeforeRecordExecutor.java) 接口自定义方法执行前的 JSON 调整，实现密码加密、自动更新时间戳、权限控制、日志、审计等。

例如实现在 Insert 时输出日志：

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

### 1.4 认证与 ApiKey

基础 Starter 内置用户认证体系，所有 API 默认需要认证后才能访问，支持 **Session 登录** 和 **ApiKey** 两种方式，任一通过即可访问。

#### ApiKey 认证

通过请求头 `X-Api-Key` 传递 ApiKey，无需登录即可访问所有 API：

```yaml
sql:
  forge:
    api-keys:                # 配置 ApiKey 列表（默认为空，表示不启用 ApiKey 认证）
      - sk-your-api-key-here
      - sk-another-key
```

请求示例：

```http request
GET http://localhost:8080/sql/forge/api/json/select/users
X-Api-Key: sk-your-api-key-here
```

#### Session 登录认证

通过登录接口获取 Session，后续请求自动携带 Session Cookie：

- `POST /sql/forge/api/auth/login` - 用户登录（Body: `{"username": "admin", "password": "admin123"}`）
- `POST /sql/forge/api/auth/logout` - 用户登出
- `GET /sql/forge/api/auth/status` - 获取当前登录状态
- `GET /sql/forge/api/auth/user` - 获取当前用户信息

> 默认管理员账户：`admin` / `admin123`

#### 认证优先级

```
请求进入 → ApiKey 有效？ → 放行
          → Session 已登录？ → 放行
          → 都不满足 → 401 拒绝
```

白名单路径（登录接口、静态资源）无需认证直接放行。

### 1.5 SQL 模板引擎

提供 SQL 模板功能，支持条件判断（`<if>`）、循环（`<foreach>`）、变量绑定（`#{var}`）等模板语法，根据参数动态生成 SQL 执行并返回结果。

#### 模板管理接口

- `PUT /sql/forge/api/template/sql` - 保存/更新 SQL 模板
  - id: 模板 ID
  - executorName: 执行器名称，默认 `database`
  - context: 模板内容
- `GET /sql/forge/api/template/sql/{id}` - 根据 ID 获取 SQL 模板
- `GET /sql/forge/api/template/sql` - 获取 SQL 模板列表
- `DELETE /sql/forge/api/template/sql/{id}` - 删除指定 ID 的 SQL 模板
- `POST /sql/forge/api/template/sql/{id}` - 执行指定 ID 的 SQL 模板（Body 为模板参数 Map）

#### 示例

模板配置：

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

执行模板：

```http request
POST http://localhost:8080/sql/forge/api/template/sql/sql-template-database
content-type: application/json

{
  "name": "alice",
  "ids": null
}
```

响应：

```json
[
  {
    "ID": "1",
    "USERNAME": "alice"
  }
]
```

#### 持久化模板

默认使用内存存储。继承 [ITemplateSqlStorage](sql-forge-template/src/main/java/cn/wubo/sql/forge/ITemplateSqlStorage.java) 实现自定义持久化。

### 1.6 Entity 模块

提供类型安全的实体操作构建器，通过 Lambda 引用实现编译期安全的字段引用，支持链式调用。

- [Entity](sql-forge-entity/src/main/java/cn/eubo/sql/forge/Entity.java) — 静态工具类，提供 `select/insert/update/delete/save/selectPage` 等入口
- [EntityExecutor](sql-forge-entity/src/main/java/cn/eubo/sql/forge/EntityExecutor.java) — 负责执行构建器的数据库操作

#### 特点

- 链式调用，代码简洁
- 通过 Lambda 表达式引用字段，编译期检查
- 构建器模式灵活配置查询条件
- 统一的数据库操作入口

#### 使用示例

定义一个用户实体类：

```java
@Data
@Table(name = "users")
public class User {
    @Id
    private String id;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "category")
    private String category;
}
```

使用 Entity 进行数据库操作：

```java
@Autowired
private EntityExecutor entityExecutor;

// 查询操作
EntitySelect<User> select = Entity.select(User.class)
                .distinct(true)
                .columns(User::getId, User::getUsername, User::getCategory)
                .orders(User::getUsername)
                .in(User::getUsername, "alice", "bob");
List<User> users = entityExecutor.run(select);

// 分页查询操作
EntitySelectPage<User> selectPage = Entity.selectPage(User.class)
        .columns(User::getId, User::getUsername, User::getCategory)
        .orders(User::getUsername)
        .page(0, 10);
SelectPageResult<User> result = entityExecutor.run(selectPage);

// 插入操作
EntityInsert<User> insert = Entity.insert(User.class)
        .set(User::getId, UUID.randomUUID().toString())
        .set(User::getUsername, "wb04307201")
        .set(User::getPassword, "123456");
entityExecutor.run(insert);

// 更新操作
EntityUpdate<User> update = Entity.update(User.class)
        .set(User::getPassword, "newpassword")
        .eq(User::getId, id);
int count = entityExecutor.run(update);

// 删除操作
EntityDelete<User> delete = Entity.delete(User.class)
        .eq(User::getId, id);
count = entityExecutor.run(delete);

// 对象保存（自动判断插入或更新）
User user = new User();
user.setUsername("wb04307201");
user.setPassword("123456");
user = entityExecutor.run(Entity.save(user));  // id 为 null，执行插入

user.setPassword("newpassword");
user = entityExecutor.run(Entity.save(user));  // id 不为 null，执行更新

// 对象删除
count = entityExecutor.run(Entity.delete(user));
```

#### 查询构建器说明

**1. 列选择**
- `column(SFunction<T, ?> column)` - 选择单个列
- `columns(SFunction<T, ?>... columns)` - 选择多个列

**2. 查询条件**
- `eq(SFunction<T, ?> column, Object value)` - 等于
- `neq(SFunction<T, ?> column, Object value)` - 不等于
- `gt(SFunction<T, ?> column, Object value)` - 大于
- `lt(SFunction<T, ?> column, Object value)` - 小于
- `gteq(SFunction<T, ?> column, Object value)` - 大于等于
- `lteq(SFunction<T, ?> column, Object value)` - 小于等于
- `like(SFunction<T, ?> column, Object value)` - 模糊匹配
- `notLike(SFunction<T, ?> column, Object value)` - 不匹配（NOT LIKE）
- `leftLike(SFunction<T, ?> column, Object value)` - 左模糊匹配
- `rightLike(SFunction<T, ?> column, Object value)` - 右模糊匹配
- `between(SFunction<T, ?> column, Object value1, Object value2)` - 在范围内
- `notBetween(SFunction<T, ?> column, Object value1, Object value2)` - 不在范围内
- `in(SFunction<T, ?> column, Object... value)` - 在集合中
- `notIn(SFunction<T, ?> column, Object... value)` - 不在集合中
- `isNull(SFunction<T, ?> column)` - 为 NULL
- `isNotNull(SFunction<T, ?> column)` - 不为 NULL

**3. 排序**
- `orderAsc(SFunction<T, ?> column)` - 升序排序
- `orderDesc(SFunction<T, ?> column)` - 降序排序
- `orders(SFunction<T, ?>... columns)` - 多列排序（默认升序）

**4. 分页**
- `page(Integer pageIndex, Integer pageSize)` - 设置分页参数

**5. 去重**
- `distinct(Boolean distinct)` - 设置是否去重

#### 对象保存（save）说明

根据 `@Id` 注解判断主键字段，如果没有主键字段则抛出 `IllegalArgumentException`。

- **插入条件**: 当主键值为 `null` 时执行插入操作
  - `String` 类型主键：自动生成 `UUID` 作为主键值
  - 其他类型主键：使用数据库自动生成的主键值
- **更新条件**: 当主键值不为 `null` 时执行更新操作
  - 使用主键值作为更新条件

> **注意**: 如果需要插入一条预先设置了主键值的新记录，请使用 `Entity.insert()` 而非 `Entity.save()`，因为 `save()` 在主键不为 `null` 时会执行更新操作。

---

## 2. sql-forge-calcite-spring-boot-starter（跨库联邦查询）

基于 [Apache Calcite](https://calcite.apache.org/) 实现跨数据库联邦查询，可以在一条 SQL 中联合查询来自 MySQL、PostgreSQL 等不同数据库的数据。

> 依赖基础 Starter（`sql-forge-spring-boot-starter`），会自动引入。

### 配置

```yaml
sql:
  forge:
    calcite:
      enabled: true                          # 启用 Calcite
      configuration: classpath:model.json    # Calcite 模型配置文件路径
      schemata:                              # 配置 schema
        - MYSQL
        - POSTGRES
```

`model.json` 用于描述 Calcite 的数据源连接信息，参考 [Apache Calcite 文档](https://calcite.apache.org/docs/model.html)。

### 使用方式

启用后会自动注册一个名为 `calcite` 的执行器，在所有 API 中通过 `executorName=calcite` 参数即可使用：

```http request
POST http://localhost:8080/sql/forge/api/json/select/orders?executorName=calcite
Content-Type: application/json

{
  "@column": ["o.id", "u.username", "p.name"],
  "@join": [
    {
      "type": "JOIN",
      "joinTable": "MYSQL_DB.users u",
      "on": "o.user_id = u.id"
    },
    {
      "type": "JOIN",
      "joinTable": "POSTGRES_DB.products p",
      "on": "o.product_id = p.id"
    }
  ]
}
```

---

## 3. sql-forge-web-spring-boot-starter（Amis + 控制台）

提供 Amis 低代码模板管理、Web Console 可视化界面和用户/角色管理。

> 依赖基础 Starter（`sql-forge-spring-boot-starter`），会自动引入。

### 3.1 Amis 模板 API

使用 [Amis](https://aisuda.bce.baidu.com/amis/zh-CN/docs/index) 配合 JSON API、SQL 模板 API 快速构建 Web 页面。

#### 模板管理接口

- `PUT /sql/forge/api/template/amis` - 保存/更新 Amis 模板
  - id: 模板 ID
  - name: 模板名称
  - description: 模板描述
  - context: Amis JSON Schema 内容
- `GET /sql/forge/api/template/amis/{id}` - 根据 ID 获取模板
- `GET /sql/forge/api/template/amis` - 获取模板列表
- `DELETE /sql/forge/api/template/amis/{id}` - 删除指定 ID 的模板

#### 示例

```http request
PUT http://localhost:8080/sql/forge/api/template/amis
content-type: application/json

{
    "id": "amis-template-users",
    "name": "用户管理",
    "context": "{ \"type\": \"page\", \"body\": { \"type\": \"crud\", ... } }"
}
```

渲染后页面：

![img.png](img.png)

#### 持久化模板

默认使用内存存储。继承 [ITemplateAmisStorage](sql-forge-web-autoconfigure/src/main/java/cn/wubo/sql/forge/ITemplateAmisStorage.java) 实现自定义持久化。

### 3.2 Web Console

提供可视化 Web 界面，访问地址：`/sql/forge/web`

- 数据库元数据查看、SQL 调试（需开启 `sql.forge.api.database.enabled=true`）
  ![img_1.png](img_1.png)
- JSON API 调试
  ![img_2.png](img_2.png)
- SQL 模板管理、调试
  ![img_3.png](img_3.png)
- Amis 模板管理、调试
  ![img_4.png](img_4.png)

### 3.3 用户与角色管理

认证体系（Session 登录 + ApiKey）已内置于基础 Starter，详见 [1.4 认证与 ApiKey](#14-认证与-apikey)。本节仅描述 Web Console 特有的用户/角色管理功能。

#### 用户管理接口（需管理员权限）

- `GET /sql/forge/api/user` - 获取用户列表
- `PUT /sql/forge/api/user` - 保存/更新用户
- `DELETE /sql/forge/api/user/{id}` - 删除用户

#### 角色管理接口

- `GET /sql/forge/api/role` - 获取角色列表
- `PUT /sql/forge/api/role` - 保存/更新角色（需管理员权限）
- `DELETE /sql/forge/api/role/{id}` - 删除角色（需管理员权限）
- `GET /sql/forge/api/role-template?role={roleId}` - 获取角色关联的模板 ID 列表
- `PUT /sql/forge/api/role-template` - 设置角色关联的模板（需管理员权限）
- `GET /sql/forge/api/user-role?userId={userId}` - 获取用户的角色 ID 列表（需管理员权限）
- `PUT /sql/forge/api/user-role` - 设置用户的角色（需管理员权限）

#### 扩展持久化存储

所有存储均默认使用内存实现，可通过实现以下接口替换为数据库持久化：

| 接口 | 说明 |
|------|------|
| [IUserStorage](sql-forge-core/src/main/java/cn/wubo/sql/forge/IUserStorage.java) | 用户存储 |
| [IUserRoleStorage](sql-forge-core/src/main/java/cn/wubo/sql/forge/IUserRoleStorage.java) | 用户-角色关联存储 |
| [IRoleStorage](sql-forge-web-autoconfigure/src/main/java/cn/wubo/sql/forge/IRoleStorage.java) | 角色存储 |
| [IRoleTemplateStorage](sql-forge-web-autoconfigure/src/main/java/cn/wubo/sql/forge/IRoleTemplateStorage.java) | 角色-模板关联存储 |

只需实现接口并注册为 Spring Bean 即可自动替换默认实现（`@ConditionalOnMissingBean`）。

---

## 4. sql-forge-mcp（AI MCP 服务器）

基于 [Model Context Protocol](https://modelcontextprotocol.io/)（MCP）协议的服务器，通过 **stdio** 传输方式将 sql-forge 后端能力暴露为 AI 工具。AI 助手（Claude Desktop、Cursor、Windsurf 等）可以通过 MCP 工具调用查询数据库元数据、执行 SQL、管理 Amis 模板。

> 需要一个运行中的 sql-forge 后端（需开启 `sql.forge.api.database.enabled=true`）。

### MCP 工具列表

| 工具 | 说明 |
|------|------|
| `getSystems` | 获取所有已配置的系统信息 |
| `getMetaDataDatabase` | 获取系统的数据库产品名称和版本 |
| `sqlForgeMetaDataTables` | 获取系统数据库中的所有表 |
| `getMetaDataTableInfo` | 获取表结构：列、主键、外键、索引 |
| `executeSQL` | 执行 SQL 查询并返回结果集 |
| `amisTemplateSave` | 保存 Amis 页面 JSON 模板配置 |

### stdio 使用方式（jbang）

使用 [jbang](https://www.jbang.dev/) 无需本地安装即可运行 MCP 服务器，在 MCP 客户端（Claude Desktop、Cursor 等）中配置如下：

```json
{
  "mcpServers": {
    "sql-forge-mcp": {
      "command": "jbang.cmd",
      "args": [
        "io.github.wb04307201:sql-forge-mcp:1.5.11",
        "--sql.forge.mcp.systems[0].name=订单系统",
        "--sql.forge.mcp.systems[0].url=http://localhost:8081",
        "--sql.forge.mcp.systems[0].description=订单系统，包含系统表：用户、角色、字典等，业务表：商品、订单、支付记录、用户地址、库存、订单物流、商品分类、商品评价等",
        "--sql.forge.mcp.systems[0].apiKey=test"
      ]
    }
  }
}
```

### 多系统配置

配置多个系统可将一个 MCP 服务器连接到多个 sql-forge 后端：

```json
{
  "mcpServers": {
    "sql-forge-mcp": {
      "command": "jbang.cmd",
      "args": [
        "io.github.wb04307201:sql-forge-mcp:1.5.11",
        "--sql.forge.mcp.systems[0].name=订单系统",
        "--sql.forge.mcp.systems[0].url=http://localhost:8081",
        "--sql.forge.mcp.systems[0].description=订单系统",
        "--sql.forge.mcp.systems[0].apiKey=test",
        "--sql.forge.mcp.systems[1].name=库存系统",
        "--sql.forge.mcp.systems[1].url=http://localhost:8082",
        "--sql.forge.mcp.systems[1].description=库存系统",
        "--sql.forge.mcp.systems[1].apiKey=test"
      ]
    }
  }
}
```

---

## 完整配置参考

```yaml
sql:
  forge:
    schemata:                      # 配置 schema 名称
      - PUBLIC
    api-keys:                      # ApiKey 列表（可选，配置后可通过 X-Api-Key 请求头免登录访问）
      - sk-your-api-key
    calcite:
      enabled: true                # 启用 Calcite 跨库联邦查询
      configuration: classpath:model.json
    api:
      database:
        enabled: true              # 启用数据库直连 API
        select-only: true          # true=仅允许 SELECT
      json:
        enabled: true              # 启用 JSON CRUD API（默认 true）
      template:
        sql:
          enabled: true            # 启用 SQL 模板 API（默认 true）
        amis:
          enabled: true            # 启用 Amis 模板 API（默认 true）
    console:
      enabled: true                # 启用 Web Console（默认 true）
```
