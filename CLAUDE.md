# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SQL Forge is a Java 17+ Spring Boot 3 library providing database operations, cross-database federated queries (via Apache Calcite), JSON-based CRUD APIs, SQL template engine, Amis low-code integration, and MCP (Model Context Protocol) server for AI tool integration. Published to Maven Central.

## Module Architecture

```
sql-forge-parent (pom)
├── sql-forge-core                        # Core: IExecutor, ExecutorService, SQL builder, DB metadata, SqlForgeProperties
├── sql-forge-record                      # JSON-based CRUD (Delete/Insert/Select/Update/SelectPage) with IBeforeRecordExecutor hooks
├── sql-forge-entity                      # Type-safe entity operations via lambda refs (Entity.select/insert/update/delete/save)
├── sql-forge-template                    # SQL template engine (Enjoy) + Amis template storage
├── sql-forge-web                         # Web console UI (static resources only, no Java code)
├── sql-forge-spring-boot-autoconfigure   # Auto-config: database executor, entity, record, JSON API, SQL/Amis template API
├── sql-forge-spring-boot-starter         # Starter: depends on autoconfigure (基础数据库抽象)
├── sql-forge-calcite-autoconfigure       # Auto-config: Calcite executor bean (conditional on sql.forge.calcite.enabled)
├── sql-forge-calcite-spring-boot-starter # Starter: depends on calcite-autoconfigure (Calcite 跨库查询, 可选)
├── sql-forge-web-autoconfigure           # Auto-config: console endpoints + web UI router (conditional on sql.forge.console.enabled)
├── sql-forge-web-spring-boot-starter     # Starter: depends on web-autoconfigure (Amis + Console, 可选)
├── sql-forge-mcp                         # Model Context Protocol server (Spring AI @Tool, standalone app)
└── sql-forge-test                        # Integration tests + sample app
```

### Starter 依赖关系

```
sql-forge-spring-boot-starter              ← 基础: 数据库抽象 + JSON API + Entity + SQL模板
sql-forge-calcite-spring-boot-starter      ← 依赖基础starter + calcite-core (可选引入)
sql-forge-web-spring-boot-starter          ← 依赖基础starter + console静态资源 (可选引入)
```

用户按需引入:
```xml
<!-- 基础数据库操作 (必须) -->
<dependency>
    <groupId>io.github.wb04307201</groupId>
    <artifactId>sql-forge-spring-boot-starter</artifactId>
    <version>...</version>
</dependency>

<!-- Calcite 跨库联邦查询 (可选) -->
<dependency>
    <groupId>io.github.wb04307201</groupId>
    <artifactId>sql-forge-calcite-spring-boot-starter</artifactId>
    <version>...</version>
</dependency>

<!-- Amis 模板 + Web Console (可选) -->
<dependency>
    <groupId>io.github.wb04307201</groupId>
    <artifactId>sql-forge-web-spring-boot-starter</artifactId>
    <version>...</version>
</dependency>
```

### Key Executor Hierarchy

- **`IExecutor`** — core interface (`executeQuery`, `executeInsert`, `executeUpdate`, metadata methods)
  - `DatabaseExecutor` — operates on the project's configured DataSource
  - `CalciteExcutor` — operates on Apache Calcite for cross-database queries
- **`ExecutorService`** — manages a list of `IExecutor` beans, routes by executor name
- **`RecordExecutor`** — wraps `ExecutorService`, adds `IBeforeRecordExecutor<T>` aspect hooks per operation type
- **`EntityExecutor`** — wraps `RecordExecutor`, provides type-safe entity builders with lambda column references

### API Endpoints

All endpoints use **Spring MVC 6 functional routing** (`org.springframework.web.servlet.function.RouterFunction<ServerResponse>`), **not** `@RestController`. Routes are split across three auto-config classes:

- **`SqlForgeConfiguration`** — JSON CRUD, SQL template, Amis template, database direct execute
- **`AuthAutoConfiguration`** — login/logout/status/user
- **`WebAutoConfiguration`** — user management, role management, console UI router

| Endpoint | Feature | Config Toggle |
|----------|---------|---------------|
| `/sql/forge/api/json/{method}/{tableName}` | JSON CRUD | `sql.forge.api.json.enabled` (default true) |
| `/sql/forge/api/template/sql/*` | SQL template CRUD+exec | `sql.forge.api.template.sql.enabled` (default true) |
| `/sql/forge/api/template/amis/*` | Amis template CRUD | `sql.forge.api.template.amis.enabled` (default true) |
| `/sql/forge/api/database/execute` | Direct SQL execution | `sql.forge.api.database.enabled` |
| `/sql/forge/api/database/metaDataTree` | DB metadata tree | `sql.forge.api.database.enabled` + `sql.forge.console.enabled` |
| `/sql/forge/api/auth/login` (POST), `/auth/logout` (POST), `/auth/status` (GET), `/auth/user` (GET) | Authentication | `sql.forge.console.enabled` (default true) |
| `/sql/forge/api/user` (GET/PUT/DELETE) | User management | `sql.forge.console.enabled` (default true) |
| `/sql/forge/api/role` (GET/PUT/DELETE) | Role management | `sql.forge.console.enabled` (default true) |
| `/sql/forge/api/user-role` (GET/PUT) | User-role binding | `sql.forge.console.enabled` (default true) |
| `/sql/forge/api/role-template` (GET/PUT) | Role-template binding | `sql.forge.console.enabled` (default true) |
| `/sql/forge/api/console/executorName` | List executor names | `sql.forge.console.enabled` (default true) |
| `/sql/forge/web` (root), `/web/login`, `/web/home` | UI entry redirects | `sql.forge.console.enabled` (default true) |
| `/sql/forge/console` | Web console UI | `sql.forge.console.enabled` (default true) |

### Key Configuration Properties

```yaml
sql:
  forge:
    schemata:                    # Configure schema names
      - PUBLIC
    calcite:
      enabled: true              # Enable Calcite federated queries
      configuration: classpath:model.json  # Calcite model JSON path
    api:
      database:
        select-only: true        # Default: only allow SELECT via database API
      json:
        enabled: true            # Enable JSON CRUD API
      template:
        sql:
          enabled: true          # Enable SQL template API
        amis:
          enabled: true          # Enable Amis template API
    console:
      enabled: true              # Enable web console UI
```

### Key Dependencies

- **Spring Boot** 3.5.14 (managed via BOM)
- **Spring AI** 1.1.7 — MCP tool annotations (`@Tool`, `@ToolParam`) in `sql-forge-mcp`
- **Apache Calcite** 1.41.0 — cross-database federated queries
- **JSqlParser** 5.3 — SQL parsing
- **Enjoy** 5.2.5 — SQL template engine (conditional logic, loops via `#{var}`, `<if>`, `<foreach>`)
- **MVEL2** 2.5.2 — expression evaluation
- **Jakarta Persistence API** 3.2.0 — `@Id`, `@Table`, `@Column` annotations for entities
- **Lombok** 1.18.46

## Build & Development Commands

```bash
# Build all modules (skip tests)
mvn clean install -DskipTests

# Build skipping Javadoc and GPG signing (faster local builds)
mvn clean install -DskipTests -Dgpg.skip

# Run all tests
mvn test

# Run a single test class
mvn test -pl sql-forge-test -Dtest=EntityExecutorDatabaseTest

# Run a single test method
mvn test -pl sql-forge-test -Dtest=EntityExecutorDatabaseTest#testSelect

# Run the test application (Spring Boot)
mvn spring-boot:run -pl sql-forge-test

# Run the MCP server (AI tool integration)
mvn spring-boot:run -pl sql-forge-mcp

# Generate Javadoc (default doclint: syntax/reference checks, missing @param does NOT fail build)
mvn javadoc:jar -pl <module-name>

# Strict mode (fails on missing @param/@return)
mvn javadoc:jar -pl <module-name> -Dmaven.javadoc.failOnError=true

# Package for Maven Central release
mvn clean install
```

## Configuration

Test app config is at `sql-forge-test/src/test/resources/application-test.yml`. Calcite model config for tests at `sql-forge-test/src/test/resources/model-test.json`. Test DB migrations use Flyway (`sql-forge-test/src/test/resources/testdb/migration/`).

## Extension Points

- **Custom executor**: implement `IExecutor`, register as Spring bean
- **Before-record hooks**: implement `IBeforeRecordExecutor<T>` for insert/update/delete/select/selectPage (e.g., logging, encryption, auto-timestamps)
- **Persistent templates**: implement `ITemplateSqlStorage` or `ITemplateAmisStorage` to replace in-memory storage
- **Entity operations**: use `Entity.select/insert/update/delete/save()` with `EntityExecutor` for type-safe chain operations
- **MCP AI tools**: add `@Tool`/`@ToolParam` annotated methods to `SqlForgeMcpService` to expose new database operations to AI clients

## Javadoc Requirements

The `maven-javadoc-plugin` (3.6.0) runs with default doclint (`-Xdoclint:all,-missing`) during `mvn install`, which checks syntax/HTML/references but does **not** fail the build on missing Javadoc. Project standard still requires complete Javadoc on all public/protected API:
- Class-level: one-sentence Chinese description
- Methods: `@param` for every parameter (including type parameters like `<T>`), `@return`, and `@throws`
- Records: `@param` for each component in the class-level Javadoc
- Enums: class-level Javadoc + inline comments (`/** ... */`) for each constant
- `@UtilityClass` (Lombok): do **not** add explicit constructors — Lombok generates them and rejects duplicates

To enforce strict mode at build time, add `<doclint>all</doclint>` and `<failOnError>true</failOnError>` to the `maven-javadoc-plugin` configuration in the parent `pom.xml`.

## Testing Notes

- Test app uses **H2 in-memory DB** with **Flyway** migrations (`sql-forge-test/src/test/resources/testdb/migration/`)
- Calcite tests are gated by `CalciteCondition` — skipped if MySQL (3306) and PostgreSQL (5432) are not both reachable on localhost
- Test config at `sql-forge-test/src/test/resources/application-test.yml` enables Calcite with `classpath:model-test.json`
- All test classes use `@SpringBootTest` with `@ActiveProfiles("test")`
