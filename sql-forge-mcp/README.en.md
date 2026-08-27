# sql-forge-mcp

Spring AI MCP server for SQL Forge — exposes the SQL Forge backend API as AI-callable Tools, and exposes Amis page-building knowledge/templates as AI-readable Resources.

> 📖 This is the **English version** of [`README.md`](README.md). For the canonical (Chinese) version, see [`README.md`](README.md).

## Module Constraints

This module **holds no web entrypoints** (no servlet / `RouterFunction`), and does **not depend on** the `sql-forge-web` module.
All Amis previews use `page.setContent()` to inject self-contained HTML inside the Java process — **no HTTP call to load the page** goes through any business backend; the Amis SDK is fetched on-demand by the browser from the jsdelivr CDN.

## MCP Three Primitives at a Glance

Per the MCP spec, capabilities are split into three primitives (**Tools** / **Resources** / **Prompts**):

- **Tools** = RPC / actions (CRUD, validation, rendering, save)
- **Resources** = read-only references (knowledge, templates, indexes — preferred for resident context)
- **Prompts** = task templates (pre-built instructions with placeholders)

An AI Agent receives all three lists. **Resources are pre-loaded if possible; Tools are invoked on demand.**

---

## 1. Resources (5, passive read-only)

AI clients see these URIs at initialization and read content by URI. The `schema-hints` plus `components`/`examples` indexes are recommended to **pre-load into context** at conversation start; detailed schemas are expanded on demand via `{type}` / `{name}` templates.

| URI | mimeType | Description |
|---|---|---|
| `amis://schema-hints` | `text/markdown` | Full Amis Schema cheat sheet (expressions / API idioms / CRUD patterns / pitfall list). **Most recommended to keep resident** |
| `amis://components` | `application/json` | Component catalog lightweight index (only `type / name / category / description`, no `keyProps`) |
| `amis://examples` | `application/json` | Example lightweight index (only `name / title / tags / description`) |
| `amis://components/{type}` | `application/json` | Full component spec by type (with `required` / `keyProps` / `minimalExample` / `docUrl`) |
| `amis://examples/{name}` | `application/json` | Full runnable JSON Schema by name |

> Resources are exposed by `AmisMcpResources` (spring-ai 1.1.x uses a programmatic `List<McpServerFeatures.SyncResourceSpecification>` Bean, not annotations).

---

## 2. Tools (29, on-demand actions)

### Amis Actions (2)
| Tool | Purpose |
|---|---|
| `validateAmisTemplate(context)` | Static validation: JSON syntax / required fields / component type / API format / nested recursion |
| `previewAmisTemplate(systemName, context)` | Real render via headless Chromium (self-contained HTML). If Chromium unavailable, degrades to `render.available=false + installHint` |

### Backend CRUD (13)
| Tool | Purpose |
|---|---|
| `getSystems` / `getMetaDataDatabase` / `sqlForgeMetaDataTables` / `getMetaDataTableInfo` / `getMetaDataTree` / `listExecutorNames` | Metadata queries |
| `jsonSelect` / `jsonSelectPage` / `jsonInsert` / `jsonUpdate` / `jsonDelete` | JSON CRUD (by `tableName` + body) |
| `findTablesByName(systemName, keyword)` | Fuzzy table name search |
| `describeSchema(systemName, tableNamePattern?)` | One-shot full schema (db → tables → columns/PKs/FKs/indexes) |
| `countRows(systemName, tableName, whereJson?)` | Row count |

### Template CRUD + SQL Execution (12)
| Tool | Purpose |
|---|---|
| `executeSQL(systemName, sql)` | Direct SQL execution (SELECT-only by default, controlled by `sql.forge.api.database.select-only`) |
| `executeSqlTemplate(systemName, id, params?)` | Execute SQL template (recommended) |
| `executeSqlTemplateSafely(systemName, id, params?)` | Same as above, but pre-validates template exists + params complete + refuses unbound-placeholder SQL |
| `saveSqlTemplate` / `getSqlTemplate` / `listSqlTemplates` / `deleteSqlTemplate` | SQL template CRUD |
| `amisTemplateSave` / `getAmisTemplate` / `listAmisTemplates` / `deleteAmisTemplate` | Amis template CRUD (persists to business backend) |

---

## Amis Agent Build Pipeline (One-shot)

The standard path: **read knowledge → pick component → modify schema → validate → render → save**:

```
1. resources/read  amis://schema-hints            (recommended resident)
2. resources/read  amis://components              (candidate list)
   └ resources/read  amis://components/{type}      (expand by type as needed)
3. resources/read  amis://examples                 (example index)
   └ resources/read  amis://examples/{name}        (full schema by name as template)
4. Modify schema (per hints section 7 mapping table)
5. tools/call      validateAmisTemplate(json)      (must pass first)
6. tools/call      previewAmisTemplate(systemName, json) (visual verification)
7. tools/call      amisTemplateSave(systemName, id, name, description, json)
```

---

## 3. Prompts (3, task templates)

Prompts are pre-built, placeholder-based instruction templates that AI clients can trigger as `/slash-command`. Three are preset:

| Prompt | Args | Purpose |
|---|---|---|
| `create-amis-page` | `<systemName> <tableName> <pageTitle>` | Standard pipeline: probe table → read schema-hints → read component spec → adapt → validate → preview |
| `diagnose-render-error` | `<context> <errors>` | Feed failed preview errors back; have AI fix the schema and try again |
| `quick-crud-template` | `<systemName> <tableName> [withFilter]` | One-shot skeleton: read-only mode, minimum dialogue to generate the simplest CRUD |

> Design philosophy: enabling "one-line CRUD generation" comes from skeleton + templates + a diagnostic loop — not from dumping 50 Tools at the Agent.

---

## Render Degradation

When Chromium is unavailable, `previewAmisTemplate` returns `render.available=false` with an `installHint`. Static validation results are unaffected.

---

## Build & Run

```bash
# Build
mvn -pl sql-forge-mcp clean install -DskipTests

# Start MCP server
mvn -pl sql-forge-mcp spring-boot:run \
    -Dspring-boot.run.jvmArguments="-Dsql.forge.mcp.systems[0].name=Test \
        -Dsql.forge.mcp.systems[0].url=http://localhost:8081 \
        -Dsql.forge.mcp.systems[0].apiKey=test"

# Test
mvn -pl sql-forge-mcp test
```

## Knowledge Resources

Hand-curated resource files live under `src/main/resources/amis/`:

- `catalog.json` — 54 components metadata (grouped by category: crud / form / input / display / others)
- `examples.json` — 17 complete runnable examples (crud-page / form-page / dialog-form / wizard / tabs / etc.)
- `schema-hints.md` — Markdown cheat sheet

After modifying them, restart the MCP service for the changes to take effect — no Java recompile needed.

## Refactor Notes (v1.7+)

The 6 Amis-knowledge Tools on `SqlForgeMcpService` (`listAmisCategories` / `listAmisComponents` /
`getAmisComponentSpec` / `listAmisExamples` / `getAmisExample` / `getAmisSchemaHints`)
have been migrated to MCP Resources, because these APIs are fundamentally "read-only reference data" rather than "actions". Moving them from Tools to Resources lets AI clients **pre-load `amis://schema-hints`** (the most-resident, highest-value Markdown cheat sheet) **into the system prompt at init time**, rather than requiring the model to actively invoke them.

---

## Complete Agent Operation Pipeline

A full 11-step pipeline example (using MCP tools to build a USERS crud-page: probe → read knowledge → assemble → validate → save → render → modify → re-save → round-trip → delete) lives at the repository root:
[`docs/agent-journey.md`](../../docs/agent-journey.md).

> 📖 The canonical (Chinese) version of this document is [`README.md`](README.md).
