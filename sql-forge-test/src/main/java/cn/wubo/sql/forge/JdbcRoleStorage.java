package cn.wubo.sql.forge;

import cn.eubo.sql.forge.Entity;
import cn.eubo.sql.forge.EntityExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class JdbcRoleStorage implements IRoleStorage {

    private final EntityExecutor entityExecutor;

    @Override
    public List<Role> list() {
        try {
            return entityExecutor.run(Entity.select(Role.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Role> list(Role filter) {
        try {
            List<Role> roles = entityExecutor.run(Entity.select(Role.class));
            if (StringUtils.hasText(filter.getName())) {
                roles = roles.stream()
                    .filter(r -> r.getName().contains(filter.getName()))
                    .collect(Collectors.toList());
            }
            return roles;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Role get(String id) {
        try {
            List<Role> roles = entityExecutor.run(
                Entity.select(Role.class).eq(Role::getId, id));
            return roles.isEmpty() ? null : roles.get(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(Role role) {
        try {
            Role existing = get(role.getId());
            if (existing == null) {
                entityExecutor.run(Entity.insert(Role.class)
                    .set(Role::getId, role.getId())
                    .set(Role::getName, role.getName())
                    .set(Role::getDescription, role.getDescription()));
            } else {
                entityExecutor.run(Entity.update(Role.class)
                    .set(Role::getName, role.getName())
                    .set(Role::getDescription, role.getDescription())
                    .eq(Role::getId, role.getId()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove(String id) {
        try {
            entityExecutor.run(Entity.delete(Role.class).eq(Role::getId, id));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
