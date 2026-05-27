package cn.wubo.sql.forge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRoleStorage implements IUserRoleStorage {

    private final Map<String, Set<String>> store = new ConcurrentHashMap<>();

    public InMemoryUserRoleStorage() {
        // 默认用户-角色关联为空
    }

    @Override
    public List<String> listRoleIdsByUser(String userId) {
        Set<String> ids = store.get(userId);
        return ids != null ? new ArrayList<>(ids) : List.of();
    }

    @Override
    public void save(UserRole userRole) {
        store.computeIfAbsent(userRole.getUserId(), k -> ConcurrentHashMap.newKeySet())
             .add(userRole.getRoleId());
    }

    @Override
    public void remove(String userId, String roleId) {
        Set<String> ids = store.get(userId);
        if (ids != null) ids.remove(roleId);
    }

    @Override
    public void removeAllByUser(String userId) {
        store.put(userId, ConcurrentHashMap.newKeySet());
    }
}
