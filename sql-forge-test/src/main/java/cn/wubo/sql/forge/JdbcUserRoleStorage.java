package cn.wubo.sql.forge;

import cn.wubo.sql.forge.map.ParamMap;
import lombok.RequiredArgsConstructor;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 ExecutorService 的用户-角色关联存储实现，直接执行 SQL 操作用户角色关联表。
 */
@RequiredArgsConstructor
public class JdbcUserRoleStorage implements IUserRoleStorage {

    private final ExecutorService executorService;

    @Override
    public List<String> listRoleIdsByUser(String userId) {
        try {
            ParamMap params = new ParamMap();
            params.put(userId);
            List<cn.wubo.sql.forge.map.RowMap> rows = executorService.getExecutor().executeQuery(
                new SqlScript("SELECT role_id FROM sql_forge_user_role WHERE user_id = ?", params));
            return rows.stream().map(row -> {
                Object value = row.get("ROLE_ID");
                if (value == null) {
                    value = row.get("role_id");
                }
                return (String) value;
            }).collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(UserRole userRole) {
        try {
            ParamMap params = new ParamMap();
            params.put(userRole.getUserId());
            params.put(userRole.getRoleId());
            executorService.getExecutor().executeUpdate(
                new SqlScript("INSERT INTO sql_forge_user_role (user_id, role_id) VALUES (?, ?)", params));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove(String userId, String roleId) {
        try {
            ParamMap params = new ParamMap();
            params.put(userId);
            params.put(roleId);
            executorService.getExecutor().executeUpdate(
                new SqlScript("DELETE FROM sql_forge_user_role WHERE user_id = ? AND role_id = ?", params));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeAllByUser(String userId) {
        try {
            ParamMap params = new ParamMap();
            params.put(userId);
            executorService.getExecutor().executeUpdate(
                new SqlScript("DELETE FROM sql_forge_user_role WHERE user_id = ?", params));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
