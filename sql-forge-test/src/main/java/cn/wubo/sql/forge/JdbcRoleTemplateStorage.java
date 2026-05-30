package cn.wubo.sql.forge;

import cn.wubo.sql.forge.map.ParamMap;
import cn.wubo.sql.forge.map.RowMap;
import lombok.RequiredArgsConstructor;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 ExecutorService 的角色-模板关联存储实现，直接执行 SQL 操作角色模板关联表。
 */
@RequiredArgsConstructor
public class JdbcRoleTemplateStorage implements IRoleTemplateStorage {

    private final ExecutorService executorService;

    @Override
    public List<String> listTemplateIdsByRole(String roleId) {
        try {
            ParamMap params = new ParamMap();
            params.put(roleId);
            List<RowMap> rows = executorService.getExecutor().executeQuery(
                new SqlScript("SELECT template_id FROM sql_forge_role_template WHERE role_id = ?", params));
            return rows.stream().map(row -> {
                Object value = row.get("TEMPLATE_ID");
                if (value == null) {
                    value = row.get("template_id");
                }
                return (String) value;
            }).collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(RoleTemplate roleTemplate) {
        try {
            ParamMap params = new ParamMap();
            params.put(roleTemplate.getRoleId());
            params.put(roleTemplate.getTemplateId());
            executorService.getExecutor().executeUpdate(
                new SqlScript("INSERT INTO sql_forge_role_template (role_id, template_id) VALUES (?, ?)", params));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove(String roleId, String templateId) {
        try {
            ParamMap params = new ParamMap();
            params.put(roleId);
            params.put(templateId);
            executorService.getExecutor().executeUpdate(
                new SqlScript("DELETE FROM sql_forge_role_template WHERE role_id = ? AND template_id = ?", params));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeAllByRole(String roleId) {
        try {
            ParamMap params = new ParamMap();
            params.put(roleId);
            executorService.getExecutor().executeUpdate(
                new SqlScript("DELETE FROM sql_forge_role_template WHERE role_id = ?", params));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
