package cn.wubo.sql.forge;

import cn.eubo.sql.forge.Entity;
import cn.eubo.sql.forge.EntityExecutor;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class JdbcUserRoleStorage implements IUserRoleStorage {

    private final EntityExecutor entityExecutor;

    @Override
    public List<String> listRoleIdsByUser(String userId) {
        try {
            List<UserRole> list = entityExecutor.run(
                Entity.select(UserRole.class).eq(UserRole::getUserId, userId));
            return list.stream().map(UserRole::getRoleId).toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(UserRole userRole) {
        try {
            entityExecutor.run(Entity.insert(UserRole.class)
                .set(UserRole::getUserId, userRole.getUserId())
                .set(UserRole::getRoleId, userRole.getRoleId()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove(String userId, String roleId) {
        try {
            entityExecutor.run(Entity.delete(UserRole.class)
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, roleId));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeAllByUser(String userId) {
        try {
            entityExecutor.run(Entity.delete(UserRole.class)
                .eq(UserRole::getUserId, userId));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
