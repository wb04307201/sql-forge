package cn.wubo.sql.forge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRoleTemplateStorage implements IRoleTemplateStorage {

    private final Map<String, Set<String>> store = new ConcurrentHashMap<>();

    public InMemoryRoleTemplateStorage() {
        // 默认角色模板关联为空
    }

    @Override
    public List<String> listTemplateIdsByRole(String roleId) {
        Set<String> ids = store.get(roleId);
        if (ids == null) return new ArrayList<>();
        return new ArrayList<>(ids);
    }

    @Override
    public void save(RoleTemplate roleTemplate) {
        Set<String> set = store.computeIfAbsent(roleTemplate.getRoleId(), k -> ConcurrentHashMap.newKeySet());
        if (set != null) {
            set.add(roleTemplate.getTemplateId());
        }
    }

    @Override
    public void remove(String roleId, String templateId) {
        Set<String> ids = store.get(roleId);
        if (ids != null) ids.remove(templateId);
    }

    @Override
    public void removeAllByRole(String roleId) {
        store.put(roleId, ConcurrentHashMap.newKeySet());
    }
}
