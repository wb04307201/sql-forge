package cn.wubo.sql.forge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InMemoryUserStorage 单元测试，覆盖 password 兜底逻辑（防止被静默清空）。
 */
class InMemoryUserStorageTest {

    private InMemoryUserStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryUserStorage();
    }

    @Test
    @DisplayName("预置 admin 账户密码为 admin123")
    void testAdminPreset() {
        User admin = storage.findByUsername("admin");
        assertNotNull(admin, "admin 账户应存在");
        assertEquals("admin123", admin.getPassword(), "默认密码应为 admin123");
        assertTrue(admin.getEnabled(), "默认启用");
        assertEquals("admin", admin.getCategory());
    }

    @Test
    @DisplayName("编辑用户时 password 字段为 null → 保留旧密码")
    void testSaveNullPasswordPreservesOld() {
        User admin = storage.findByUsername("admin");
        String oldId = admin.getId();

        // 模拟前端编辑：admin 用户未提供 password 字段
        User update = new User();
        update.setId(oldId);
        update.setUsername("admin");
        update.setPassword(null);   // 关键：模拟前端未传 password
        update.setEnabled(true);
        update.setCategory("admin");
        storage.save(update);

        // 验证：密码应保留
        User after = storage.findByUsername("admin");
        assertEquals("admin123", after.getPassword(), "password 为 null 时应保留原密码，不能被清空");
        assertEquals(oldId, after.getId());
    }

    @Test
    @DisplayName("编辑用户时显式提供新 password → 更新成功")
    void testSaveNewPasswordUpdates() {
        User admin = storage.findByUsername("admin");

        User update = new User();
        update.setId(admin.getId());
        update.setUsername("admin");
        update.setPassword("newSecret456");
        update.setEnabled(true);
        update.setCategory("admin");
        storage.save(update);

        User after = storage.findByUsername("admin");
        assertEquals("newSecret456", after.getPassword(), "显式传新密码应覆盖");
    }

    @Test
    @DisplayName("新增用户自动生成 id 并保存 password")
    void testSaveNewUserAutoId() {
        User newUser = new User();
        newUser.setUsername("alice");
        newUser.setPassword("alicePass");
        newUser.setEnabled(true);
        newUser.setCategory("user");
        assertNull(newUser.getId(), "新用户 id 应为 null");
        storage.save(newUser);

        assertNotNull(newUser.getId(), "保存后应自动生成 id");
        User found = storage.findByUsername("alice");
        assertNotNull(found);
        assertEquals("alicePass", found.getPassword());
    }
}