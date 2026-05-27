package cn.wubo.sql.forge;

import java.util.List;

/**
 * 角色存储接口，定义角色的列表、查询、保存和删除操作。
 */
public interface IRoleStorage {
    List<Role> list();
    List<Role> list(Role filter);
    Role get(String id);
    void save(Role role);
    void remove(String id);
}
