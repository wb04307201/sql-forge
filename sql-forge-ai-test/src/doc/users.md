# 用户维护界面需求规格说明书 (基于 Baidu Amis)

## 1. 文档概述
本文档旨在描述“用户维护管理界面”的功能需求、数据结构、接口定义及 UI 交互逻辑。本文档专为指导大语言模型（LLM）生成准确的 **Baidu Amis JSON 配置代码** 而设计。开发时需严格遵循 Amis 组件规范及后端 API 数据协议。

## 2. 页面架构设计
*   **页面类型**: `page`
*   **核心组件**: `crud` (增删改查表格)
*   **组件 ID**: `crud_table`
*   **布局特性**:
    *   自动填充高度 (`autoFillHeight`: true)
    *   显示序号列 (`showIndex`: true)
    *   主键字段 (`primaryField`): `ID`
    *   翻页保持选中 (`keepItemSelectionOnPageChange`: true)

## 3. 数据模型与接口协议
本系统后端采用 **SQL Forge API** 协议，所有接口请求需遵循特定的 JSON 结构（包含 `@column`, `@where`, `@set` 等元数据）。

### 3.1 核心数据表
*   **主表**: `USERS`
*   **字典表**: `sys_dict_items` (别名：`sex`)
*   **关联关系**: `USERS.DICT_SEX` = `sys_dict_items.item_code`

### 3.2 接口定义清单

| 功能 | 接口路径 | 方法 | 说明 | 关键参数映射 |
| :--- | :--- | :--- | :--- | :--- |
| **列表查询** | `/sql/forge/api/json/selectPage/USERS` | POST | 分页查询用户 | 需包含 `@page`, `@order`, `@join` (关联性别字典) |
| **新增用户** | `/sql/forge/api/json/insert/USERS` | POST | 插入单条数据 | `@set` 包含 `ID` (UUID), `USERNAME`, `DICT_SEX`, `EMAIL` |
| **修改用户** | `/sql/forge/api/json/update/USERS` | POST | 更新单条数据 | `@set` 包含更新字段，`@where` 锁定 `ID` |
| **删除用户** | `/sql/forge/api/json/delete/USERS` | POST | 删除数据 | `@where` 条件为 `ID IN (...)` 或 `ID = ...` |
| **详情查询** | `/sql/forge/api/json/select/USERS` | POST | 编辑前回显数据 | `@where` 锁定 `ID`，需配置 `responseData` 取第一条 |
| **字典数据** | `/sql/forge/api/json/select/sys_dict_items` | POST | 获取性别选项 | `@where` 条件 `dict_code = 'sex'` |
| **导出 Excel** | `/sql/forge/api/json/select/USERS` | POST | 全量数据导出 | 结构与列表查询类似，但不含分页 |

## 4. 功能模块详细需求

### 4.1 列表与表格 (CRUD Body)
*   **列定义 (Columns)**:
    1.  **ID**: 隐藏列 (`hidden`: true)，作为主键。
    2.  **用户名 (USERNAME)**: 文本显示，支持排序 (`sortable`)，支持搜索。
    3.  **性别 (SEX)**: 显示字典文本 (`item_name`)，支持排序，支持搜索（多选）。
    4.  **邮箱 (EMAIL)**: 文本显示，支持排序，支持搜索。
    5.  **操作列**: 固定在右侧 (`fixed`: "right")，包含“修改”和“删除”按钮。
*   **分页 (Pagination)**:
    *   布局：总数、每页条数、页码、跳转 (`total,perPage,pager,go`)。
    *   参数映射：`pageIndex` 对应 `${page - 1}`, `pageSize` 对应 `${perPage}`。
*   **排序**:
    *   支持前端触发，后端接收 `orderBy` 和 `orderDir` 变量。

### 4.2 搜索与过滤 (Filter)
*   **触发方式**: 开启 `autoGenerateFilter: true`，并在列定义中配置 `searchable`。
*   **搜索字段**:
    1.  **用户名**: 模糊查询 (`LIKE`)，输入框，最大长度 50。
    2.  **性别**: 包含查询 (`IN`)，下拉多选框 (`multiple`: true)，数据源来自字典表。
    3.  **邮箱**: 模糊查询 (`LIKE`)，输入框，最大长度 100。
*   **默认值处理**: 所有搜索值需处理 `default:undefined`，空值不传入查询条件。

### 4.3 新增功能 (Create)
*   **入口**: 顶部工具栏“新增”按钮。
*   **交互形式**: 侧边抽屉 (`actionType`: "drawer")。
*   **表单字段**:
    1.  **ID**: 类型 `uuid`，自动生成，不可见或只读。
    2.  **用户名**: 文本输入，最大长度 50。
    3.  **性别**: 下拉选择 (`select`)，数据源动态获取，支持清空 (`clearable`)。
    4.  **邮箱**: 文本输入，最大长度 100。
*   **提交逻辑**:
    *   提交成功后 (`submitSucc`)，触发 `reload` 动作刷新 `crud_table`。
    *   API 提交数据需映射到 `@set` 对象。

### 4.4 修改功能 (Update)
*   **入口**: 表格操作列“修改”按钮。
*   **交互形式**: 侧边抽屉 (`actionType`: "drawer")。
*   **数据回显**:
    *   配置 `initApi`，根据当前行 `ID` 查询详情。
    *   **关键配置**: `responseData` 需设置为 `{ "&": "${items | first}" }` 以扁平化数据。
*   **表单字段**: 同新增表单，但 ID 字段为隐藏 (`hidden`: true) 的文本框。
*   **提交逻辑**:
    *   API 提交需包含 `@where` 条件锁定 `ID`。
    *   提交成功后刷新表格。

### 4.5 删除功能 (Delete)
*   **单条删除**:
    *   位置：操作列。
    *   交互：点击后弹出确认框 (`confirmText`: "确认要删除？")。
    *   API：`@where` 条件 `ID = ${ID}`。
*   **批量删除**:
    *   位置：表格批量操作栏 (`bulkActions`)。
    *   交互：选中多行后显示，点击弹出确认框 (`confirmText`: "确定要批量删除？")。
    *   API：`@where` 条件 `ID IN (${ids | split})`。

### 4.6 导出功能 (Export)
*   **位置**: 顶部工具栏右侧。
*   **组件类型**: `export-excel`。
*   **数据范围**: 受当前搜索条件过滤影响。
*   **API**: 复用列表查询的 SQL 逻辑，但不包含分页参数。

## 5. UI/UX 细节规范
*   **图标库**: 使用 FontAwesome (`fa fa-plus`, `fa fa-trash`, `fa fa-pen-to-square` 等)。
*   **按钮等级**:
    *   新增：`level`: "primary"
    *   删除：`level`: "danger"
*   **表单验证**:
    *   文本字段需设置 `maxLength` (用户名 50, 性别 100, 邮箱 100)。
    *   下拉框需设置 `clearable`: true。
*   **工具栏布局**:
    *   左侧：新增按钮。
    *   中间：批量操作。
    *   右侧：列切换 (`columns-toggler`)、导出按钮。

## 6. 数据字典与适配器 (Adaptor)
*   **性别字典**:
    *   字典编码：`sex`
    *   值字段：`item_code`
    *   标签字段：`item_name`
*   **API 适配器 (Adaptor)**:
    *   在获取下拉选项的 API 中，必须配置 `adaptor` 脚本，将后端返回的 `payload` 转换为 Amis 需要的 `options` 格式。
    *   **脚本逻辑**:
        ```javascript
        return {
          options: payload.map(item => ({
            value: item.item_code || item.ITEM_CODE,
            label: item.item_name || item.ITEM_NAME
          }))
        };
        ```

## 7. Amis 配置生成约束 (针对 LLM)
在生成 JSON 配置时，请严格遵守以下规则：
1.  **API 数据结构**: 所有 `api.data` 必须保留 `@column`, `@where`, `@set`, `@join`, `@page` 等特定键名，不可更改为常规 REST 风格。
2.  **变量映射**: 严格使用 `${变量名}` 语法，注意 `default:undefined` 和 `split` 过滤器的使用。
3.  **组件 ID**: 保持关键组件 ID 唯一性（如 `crud_table`, `insert-USERNAME`, `update-ID` 等），以便事件联动。
4.  **事件联动**: 确保 `submitSucc` 事件正确绑定到 `reload` 动作，目标组件 ID 为 `crud_table`。
5.  **Join 语法**: SQL Join 配置需严格匹配 `type`, `joinTable`, `on` 结构。
6.  **响应处理**: 编辑回显的 `initApi` 必须包含 `responseData` 处理逻辑，确保表单能正确获取字段值。

---
**使用说明**: 将上述需求文档提供给大模型，并附加指令：“请根据以上需求文档，生成完整的 Baidu Amis JSON 配置代码，确保 API 协议和字段映射与需求完全一致。”