package cn.wubo.sql.forge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryUserStorage implements IUserStorage {

    private final Map<String, User> store = new ConcurrentHashMap<>();

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
        store.put(user.getId(), user);
    }

    @Override
    public void remove(String id) {
        store.remove(id);
    }
}
