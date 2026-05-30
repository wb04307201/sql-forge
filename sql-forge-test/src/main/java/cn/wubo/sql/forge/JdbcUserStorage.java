package cn.wubo.sql.forge;

import cn.wubo.sql.forge.map.ParamMap;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 基于 ExecutorService 的用户存储实现，直接执行 SQL 操作用户表。
 */
@RequiredArgsConstructor
public class JdbcUserStorage implements IUserStorage {

    private final ExecutorService executorService;

    @Override
    public User findByUsername(String username) {
        try {
            ParamMap params = new ParamMap();
            params.put(username);
            List<cn.wubo.sql.forge.map.RowMap> rows = executorService.getExecutor().executeQuery(
                new SqlScript("SELECT id, username, password, enabled, category FROM users WHERE username = ?", params));
            return rows.isEmpty() ? null : mapRow(rows.get(0));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<User> list(User filter) {
        try {
            String sql = "SELECT id, username, password, enabled, category FROM users";
            ParamMap params = new ParamMap();
            if (StringUtils.hasText(filter.getUsername())) {
                sql += " WHERE username LIKE ?";
                params.put("%" + filter.getUsername() + "%");
            }
            List<cn.wubo.sql.forge.map.RowMap> rows = executorService.getExecutor().executeQuery(
                new SqlScript(sql, params));
            return rows.stream().map(this::mapRow).collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(User user) {
        try {
            User existing = findByUsername(user.getUsername());
            if (existing == null) {
                if (user.getId() == null) {
                    user.setId(UUID.randomUUID().toString());
                }
                ParamMap params = new ParamMap();
                params.put(user.getId());
                params.put(user.getUsername());
                params.put(user.getPassword());
                params.put(user.getEnabled());
                params.put(user.getCategory());
                executorService.getExecutor().executeUpdate(
                    new SqlScript("INSERT INTO users (id, username, password, enabled, category) VALUES (?, ?, ?, ?, ?)", params));
            } else {
                user.setId(existing.getId());
                ParamMap params = new ParamMap();
                params.put(user.getUsername());
                params.put(user.getPassword());
                params.put(user.getEnabled());
                params.put(user.getCategory());
                params.put(existing.getId());
                executorService.getExecutor().executeUpdate(
                    new SqlScript("UPDATE users SET username = ?, password = ?, enabled = ?, category = ? WHERE id = ?", params));
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
                new SqlScript("DELETE FROM users WHERE id = ?", params));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private User mapRow(cn.wubo.sql.forge.map.RowMap row) {
        User user = new User();
        user.setId(getString(row, "id"));
        user.setUsername(getString(row, "username"));
        user.setPassword(getString(row, "password"));
        user.setEnabled(getBoolean(row, "enabled"));
        user.setCategory(getString(row, "category"));
        return user;
    }

    private String getString(cn.wubo.sql.forge.map.RowMap row, String column) {
        Object value = row.get(column.toUpperCase());
        if (value == null) {
            value = row.get(column.toLowerCase());
        }
        return (String) value;
    }

    private Boolean getBoolean(cn.wubo.sql.forge.map.RowMap row, String column) {
        Object value = row.get(column.toUpperCase());
        if (value == null) {
            value = row.get(column.toLowerCase());
        }
        return (Boolean) value;
    }
}
