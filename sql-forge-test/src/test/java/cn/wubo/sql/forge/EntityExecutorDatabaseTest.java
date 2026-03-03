package cn.wubo.sql.forge;

import cn.eubo.sql.forge.Entity;
import cn.eubo.sql.forge.EntityExecutor;
import cn.eubo.sql.forge.entity.*;
import cn.wubo.sql.forge.record.SelectPageResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class EntityExecutorDatabaseTest {

    @Autowired
    EntityExecutor entityExecutor;

    @Test
    void testEntitySelect() throws Exception {
        EntitySelect<User> select = Entity.select(User.class)
                .distinct(true)
                .columns(User::getId, User::getUsername, User::getEmail)
                .orders(User::getUsername)
                .in(User::getUsername, "alice", "bob");
        List<User> users = entityExecutor.run(select);
        log.info("{}", users);
        assertEquals(2, users.size());
    }

    @Test
    void testEntitySelectPage() throws Exception {
        EntitySelectPage<User> select = Entity.selectPage(User.class)
                .distinct(true)
                .columns(User::getId, User::getUsername, User::getEmail)
                .orders(User::getUsername)
                .in(User::getUsername, "alice", "bob")
                .page(0, 1);
        SelectPageResult<User> users = entityExecutor.run(select);
        log.info("{}", users);
        assertEquals(2, users.total());
        assertEquals(1, users.rows().size());
    }

    @Test
    void testEntity() throws Exception {
        User user = new User();
        user.setUsername("EntityExecutorDatabaseTest");
        user.setEmail("wb04307201@gitee.com");
        user = entityExecutor.run(Entity.save(user));
        log.info("{}", user);
        user.setEmail("wb04307201@github.com");
        user = entityExecutor.run(Entity.save(user));
        log.info("{}", user);
        int count = entityExecutor.run(Entity.delete(user));
        log.info("{}", count);
    }

    @Test
    void test() throws Exception {
        String id = UUID.randomUUID().toString();
        EntityInsert<User> insert = Entity.insert(User.class).set(User::getId, id)
                .set(User::getUsername, "wb04307201")
                .set(User::getEmail, "wb04307201@gitee.com");
        Object key = entityExecutor.run(insert);
        log.info("{}", key);

        EntityUpdate<User> update = Entity.update(User.class)
                .set(User::getEmail, "wb04307201@github.com")
                .eq(User::getId, id);
        int count = entityExecutor.run(update);
        log.info("{}", count);

        EntityDelete<User> delete = Entity.delete(User.class)
                .eq(User::getId, id);
        count = entityExecutor.run(delete);
        log.info("{}", count);
    }
}
