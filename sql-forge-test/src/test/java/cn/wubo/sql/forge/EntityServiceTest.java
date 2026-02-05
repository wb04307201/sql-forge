package cn.wubo.sql.forge;

import cn.wubo.sql.forge.entity.*;
import cn.wubo.sql.forge.record.SelectPageResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class EntityServiceTest {

    @Autowired
    private EntityService entityService;

    @Test
    void testEntitySelect() throws Exception {
        EntitySelect<User> select = Entity.select(User.class)
                .distinct(true)
                .columns(User::getId, User::getUsername, User::getEmail)
                .orders(User::getUsername)
                .in(User::getUsername, "alice", "bob");
        List<User> users = entityService.run(select);
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
        SelectPageResult<User> users = entityService.run(select);
        log.info("{}", users);
        assertEquals(2, users.total());
        assertEquals(1, users.rows().size());
    }

    @Test
    void testEntity() throws Exception {
        User user = new User();
        user.setUsername("wb04307201");
        user.setEmail("wb04307201@gitee.com");
        user.setPassword("123456");
        user = entityService.run(Entity.save(user));
        log.info("{}", user);
        user.setEmail("wb04307201@github.com");
        user = entityService.run(Entity.save(user));
        log.info("{}", user);
        int count = entityService.run(Entity.delete(user));
        log.info("{}", count);
    }

    @Test
    void test() throws Exception {
        String id = UUID.randomUUID().toString();
       EntityInsert<User> insert = Entity.insert(User.class).set(User::getId, id)
                .set(User::getUsername, "wb04307201")
                .set(User::getEmail, "wb04307201@gitee.com")
               .set(User::getPassword, "123456");
       Object key = entityService.run(insert);
       log.info("{}", key);

        EntityUpdate<User> update = Entity.update(User.class)
                .set(User::getEmail, "wb04307201@github.com")
                .eq(User::getId, id);
        int count = entityService.run(update);
        log.info("{}", count);

        EntityDelete<User> delete = Entity.delete(User.class)
                .eq(User::getId, id);
        count = entityService.run(delete);
        log.info("{}", count);
    }
}
