package cn.wubo.sql.forge.agent;

import cn.wubo.sql.forge.amis.AmisValidator.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Journey 3：多表泛化（USERS / ORDERS / PRODUCTS 三个表都能走 CRUD journey）。
 * <p>
 * 验证 workflow 对不同 schema 都能工作（不依赖具体字段名）。
 * </p>
 */
class MultiTableJourneyTest extends AgentJourneyBaseTest {

    /**
     * 对 3 张表分别跑 CRUD journey，验证 schema 拼装是泛化的。
     * <p>
     * 所有 mock 期望在循环前一次性设好（MockRestServiceServer 限制）。
     * </p>
     */
    @Test
    @DisplayName("MultiTable: USERS / ORDERS / PRODUCTS 三张表都跑通 CRUD")
    void threeTables_crudJourney_works() throws Exception {
        String[] tables = {"USERS", "ORDERS", "PRODUCTS"};

        // === 一次性设好所有 mock 期望 ===
        for (String table : tables) {
            mock.expectMetaDataTables(tableMetaFor(table));
            mock.expectMetaDataDefinitions(schemaJsonFor(table));
            mock.expectAmisTemplateSave("true");
            mock.expectAmisTemplateDelete("multi_" + table.toLowerCase());
        }

        for (String table : tables) {
            // Step 1: 拿 schema
            Object schemaDef = metadataService.describeSchema("TestSys", table);
            assertNotNull(schemaDef, table + " schema 不应为 null");

            // Step 2: 拼通用 CRUD schema
            String crudSchema = buildGenericCrudSchema(table);
            assertNotNull(crudSchema, table + " 通用 schema 不应为 null");

            // Step 3: 校验 schema
            ValidationResult vr = amisService.validateAmisTemplate(crudSchema);
            assertTrue(vr.valid(),
                    table + " 通用CRUD schema 应通过校验，errors=" + vr.errors());

            // Step 4: 保存 + 删除
            String id = "multi_" + table.toLowerCase();
            Object saved = templateService.amisTemplateSave(
                    "TestSys", id, table + " 测试", "Journey 3 多表测试", crudSchema);
            assertEquals("true", saved, table + " 保存应成功");

            Object deleted = templateService.deleteAmisTemplate("TestSys", id);
            assertEquals("true", deleted, table + " 删除应成功");
        }
    }

    /**
     * 通用 CRUD schema 拼装（基于表名，不依赖具体字段名）。
     * <p>
     * 模拟 AI Agent 根据表名生成最简 CRUD 的逻辑。
     * </p>
     */
    private String buildGenericCrudSchema(String table) {
        return String.format("""
                {
                  "type": "page",
                  "title": "%s 管理",
                  "body": {
                    "type": "crud",
                    "api": "POST /sql/forge/api/json/select/%s",
                    "columns": [
                      {"name": "ID", "label": "ID"}
                    ]
                  }
                }""", table, table);
    }
}
