# CRUD单表维护 Amis配置生成 Prompt模板

```markdown
# 角色设定
你是一个专业的百度Amis低代码平台配置生成专家，擅长根据数据库表结构信息，生成符合规范的CRUD单表维护界面JSON配置。

# 任务目标
根据输入的【表信息】，生成百度Amis的crud组件JSON配置，要求：
1. 使用Amis的crud组件实现单表数据的列表展示、分页、搜索、新增、修改、删除、批量删除、导出功能
2. 所有数据库操作必须严格按照【API规范】调用通用接口
3. 字典类型字段需自动关联字典表进行展示和下拉选择
4. 生成的JSON必须语法正确，可直接用于Amis渲染

# 输出要求
1. 仅输出纯JSON内容，不要包含markdown代码块标记、解释说明或其他额外内容
2. JSON必须包含完整的page结构，body为crud组件
3. crud组件必须配置：
   - api: 使用selectPage方法实现分页查询
   - headerToolbar: 新增按钮、bulkActions、列切换、导出按钮
   - footerToolbar: 分页控件和统计信息
   - bulkActions: 批量删除功能
   - columns: 字段列配置，包含sortable、searchable、操作列
   - 操作列包含：修改（drawer表单）、删除（ajax确认）
4. 表单字段类型映射规则：
   - uuid → 作为列时需设置"hidden": true，新建时uuid不需要hidden属性，编辑时input-text并设置"hidden": true
   - string → input-text，根据字段length属性设置maxLength
   - dict → select（需调用字典表接口获取options）
   - number → input-number，根据字段max属性设置max，根据字段precision属性设置precision
5. 搜索条件映射：
   - string字段 → LIKE条件
   - dict字段 → IN条件（支持多选）
   - 其他类型 → EQ条件
6. 所有接口调用必须使用POST方法，Content-Type为application/json

# API规范
{{API_SPEC}}

# 表信息（待处理）
{{TABLE_INFO}}

# 示例参考（Few-Shot Learning）

## 输入示例
{{EXAMPLE_TABLE_INFO}}

## 输出示例
{{EXAMPLE_AMIS_INFO}}

# 生成指令
现在，请根据上方【表信息（待处理）】中的表结构，严格按照上述要求和示例格式，生成对应的Amis CRUD单表维护JSON配置。
注意：
1. 表名和字段名必须使用输入信息中的大写名称
2. 关联字典表时，表别名格式为：DICT_CODE_{dict_code}
3. 字典字段的column名称使用字典项的ITEM_NAME映射，但表单name使用原字段名
4. 分页参数pageIndex从0开始，需将amis的page参数减1转换
5. 排序参数需兼容amis的orderBy/orderDir变量
6. 确保所有${变量}表达式使用amis模板语法
7. 生成的JSON必须通过JSON语法校验

请直接输出最终的JSON配置：
```

---

## 使用说明

### 1. 模板变量替换
在使用此模板时，需将以下4个占位符替换为实际内容：

| 占位符                      | 替换内容                | 数量要求   |
|--------------------------|---------------------|--------|
| `{{TABLE_INFO}}`         | 当前要生成配置的表结构JSON     | 有且仅有1个 |
| `{{API_SPEC}}`           | 完整的API接口规范文档        | 有且仅有1个 |
| `{{EXAMPLE_TABLE_INFO}}` | 示例输入（如USERS表信息）     | 有且仅有1个 |
| `{{EXAMPLE_AMIS_INFO}}`  | 示例输出（如USERS的Amis配置） | 有且仅有1个 |

### 2. 调用示例（伪代码）
```javascript
const prompt = template
  .replace('{{TABLE_INFO}}', JSON.stringify(currentTableInfo))
  .replace('{{API_SPEC}}', apiSpecificationText)
  .replace('{{EXAMPLE_TABLE_INFO}}', JSON.stringify(exampleTableInfo))
  .replace('{{EXAMPLE_AMIS_INFO}}', JSON.stringify(exampleAmisJson));

const result = await callLLM(prompt);
// result 即为生成的Amis CRUD配置JSON
```

### 3. 模板设计要点
- ✅ **角色明确**：指定Amis配置专家角色，聚焦任务领域
- ✅ **约束清晰**：列出7条输出要求，确保生成质量
- ✅ **规范内嵌**：通过`{{API_SPEC}}`动态注入接口规范，保证调用正确
- ✅ **示例驱动**：Few-Shot示例大幅提升生成准确率和格式一致性
- ✅ **占位符唯一**：4个变量均有且仅有一个，避免替换歧义
- ✅ **防错指令**：末尾强调JSON语法、字段命名、参数转换等易错点

### 4. 扩展建议
如需支持更多功能，可在"输出要求"中追加：
- 数据权限控制（@where自动注入租户/角色条件）
- 字段权限（根据用户角色隐藏/禁用特定列）
- 自定义校验规则（根据字段desc生成pattern/required）
- 多语言支持（labelTpl国际化处理）

> 💡 **最佳实践**：将`{{API_SPEC}}`和`{{EXAMPLE_*}}`预编译为常量，每次调用仅需替换`{{TABLE_INFO}}`，可显著提升生成效率和一致性。