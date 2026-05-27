package cn.wubo.sql.forge;

import java.util.List;

public interface IRoleStorage {
    List<Role> list();
    List<Role> list(Role filter);
    Role get(String id);
    void save(Role role);
    void remove(String id);
}
