package cn.wubo.sql.forge;

import cn.eubo.sql.forge.Entity;
import cn.eubo.sql.forge.EntityExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class JdbcUserStorage implements IUserStorage {

    private final EntityExecutor entityExecutor;

    @Override
    public User findByUsername(String username) {
        try {
            List<User> users = entityExecutor.run(
                Entity.select(User.class).eq(User::getUsername, username));
            return users.isEmpty() ? null : users.get(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<User> list(User filter) {
        try {
            var select = Entity.select(User.class);
            if (StringUtils.hasText(filter.getUsername())) {
                select.like(User::getUsername, filter.getUsername());
            }
            return entityExecutor.run(select);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(User user) {
        try {
            User existing = findByUsername(user.getUsername());
            if (existing != null) {
                user.setId(existing.getId());
                user.setCreatedTime(existing.getCreatedTime());
            }
            user.setUpdatedTime(java.time.LocalDateTime.now().toString());
            if (existing == null) {
                user.setCreatedTime(user.getUpdatedTime());
                // New user: use insert with explicit sets since Entity.save() does UPDATE when ID is set
                entityExecutor.run(Entity.insert(User.class)
                    .set(User::getId, user.getId())
                    .set(User::getUsername, user.getUsername())
                    .set(User::getPassword, user.getPassword())
                    .set(User::getEnabled, user.getEnabled())
                    .set(User::getCategory, user.getCategory())
                    .set(User::getCreatedTime, user.getCreatedTime())
                    .set(User::getUpdatedTime, user.getUpdatedTime()));
            } else {
                entityExecutor.run(Entity.save(user));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove(String id) {
        try {
            entityExecutor.run(Entity.delete(User.class).eq(User::getId, id));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
