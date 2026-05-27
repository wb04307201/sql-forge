package cn.wubo.sql.forge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryRoleStorage implements IRoleStorage {

    private final Map<String, Role> store = new ConcurrentHashMap<>();

    public InMemoryRoleStorage() {
        // 默认角色数据为空
    }

    @Override
    public List<Role> list() {
        return list(new Role());
    }

    @Override
    public List<Role> list(Role filter) {
        return store.values().stream()
            .filter(r -> filter.getName() == null || r.getName().contains(filter.getName()))
            .collect(Collectors.toList());
    }

    @Override
    public Role get(String id) {
        return store.get(id);
    }

    @Override
    public void save(Role role) {
        store.put(role.getId(), role);
    }

    @Override
    public void remove(String id) {
        store.remove(id);
    }
}
