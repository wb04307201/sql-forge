package cn.wubo.sql.forge;

import cn.eubo.sql.forge.Entity;
import cn.eubo.sql.forge.EntityExecutor;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class JdbcRoleTemplateStorage implements IRoleTemplateStorage {

    private final EntityExecutor entityExecutor;

    @Override
    public List<String> listTemplateIdsByRole(String roleId) {
        try {
            List<RoleTemplate> list = entityExecutor.run(
                Entity.select(RoleTemplate.class).eq(RoleTemplate::getRoleId, roleId));
            return list.stream().map(RoleTemplate::getTemplateId).toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(RoleTemplate roleTemplate) {
        try {
            entityExecutor.run(Entity.insert(RoleTemplate.class)
                .set(RoleTemplate::getRoleId, roleTemplate.getRoleId())
                .set(RoleTemplate::getTemplateId, roleTemplate.getTemplateId()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove(String roleId, String templateId) {
        try {
            entityExecutor.run(Entity.delete(RoleTemplate.class)
                .eq(RoleTemplate::getRoleId, roleId)
                .eq(RoleTemplate::getTemplateId, templateId));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeAllByRole(String roleId) {
        try {
            entityExecutor.run(Entity.delete(RoleTemplate.class)
                .eq(RoleTemplate::getRoleId, roleId));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
