package cn.wubo.sql.forge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于内存的用户存储默认实现，内置一个管理员账户（admin/admin123）。
 */
public class InMemoryUserStorage implements IUserStorage {

    private final Map<String, User> store = new ConcurrentHashMap<>();

    /**
     * 构造内存用户存储，预置管理员账户。
     */
    public InMemoryUserStorage() {
        User admin = new User();
        admin.setId("admin-001");
        admin.setUsername("admin");
        admin.setPassword("admin123");
        admin.setEnabled(true);
        admin.setCategory("admin");
        store.put(admin.getId(), admin);
    }

    @Override
    public User findByUsername(String username) {
        return store.values().stream()
            .filter(u -> u.getUsername().equals(username))
            .findFirst().orElse(null);
    }

    @Override
    public List<User> list(User filter) {
        return store.values().stream()
            .filter(u -> filter.getUsername() == null || u.getUsername().contains(filter.getUsername()))
            .collect(Collectors.toList());
    }

    @Override
    public void save(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }
        // 兜底：如果 password 是 null 但库里已有该用户，保留旧密码，避免被静默清空
        User existing = store.get(user.getId());
        if (existing != null && user.getPassword() == null) {
            user.setPassword(existing.getPassword());
        }
        store.put(user.getId(), user);
    }

    @Override
    public void remove(String id) {
        store.remove(id);
    }
}
