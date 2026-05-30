package cn.wubo.sql.forge;

import cn.wubo.sql.forge.map.ParamMap;
import cn.wubo.sql.forge.map.RowMap;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 ExecutorService 的角色存储实现，直接执行 SQL 操作角色表。
 */
@RequiredArgsConstructor
public class JdbcRoleStorage implements IRoleStorage {

    private final ExecutorService executorService;

    @Override
    public List<Role> list() {
        try {
            List<RowMap> rows = executorService.getExecutor().executeQuery(
                new SqlScript("SELECT id, name, description FROM sql_forge_role", new ParamMap()));
            return rows.stream().map(this::mapRow).collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Role> list(Role filter) {
        List<Role> roles = list();
        if (StringUtils.hasText(filter.getName())) {
            roles = roles.stream()
                .filter(r -> r.getName().contains(filter.getName()))
                .collect(Collectors.toList());
        }
        return roles;
    }

    @Override
    public Role get(String id) {
        try {
            ParamMap params = new ParamMap();
            params.put(id);
            List<RowMap> rows = executorService.getExecutor().executeQuery(
                new SqlScript("SELECT id, name, description FROM sql_forge_role WHERE id = ?", params));
            return rows.isEmpty() ? null : mapRow(rows.get(0));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(Role role) {
        try {
            Role existing = get(role.getId());
            if (existing == null) {
                ParamMap params = new ParamMap();
                params.put(role.getId());
                params.put(role.getName());
                params.put(role.getDescription());
                executorService.getExecutor().executeUpdate(
                    new SqlScript("INSERT INTO sql_forge_role (id, name, description) VALUES (?, ?, ?)", params));
            } else {
                ParamMap params = new ParamMap();
                params.put(role.getName());
                params.put(role.getDescription());
                params.put(role.getId());
                executorService.getExecutor().executeUpdate(
                    new SqlScript("UPDATE sql_forge_role SET name = ?, description = ? WHERE id = ?", params));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove(String id) {
        try {
            ParamMap params = new ParamMap();
            params.put(id);
            executorService.getExecutor().executeUpdate(
                new SqlScript("DELETE FROM sql_forge_role WHERE id = ?", params));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Role mapRow(RowMap row) {
        Role role = new Role();
        role.setId(getString(row, "id"));
        role.setName(getString(row, "name"));
        role.setDescription(getString(row, "description"));
        return role;
    }

    private String getString(RowMap row, String column) {
        Object value = row.get(column.toUpperCase());
        if (value == null) {
            value = row.get(column.toLowerCase());
        }
        return (String) value;
    }
}
