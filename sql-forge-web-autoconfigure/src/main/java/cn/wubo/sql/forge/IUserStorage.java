package cn.wubo.sql.forge;

import java.util.List;

public interface IUserStorage {
    User findByUsername(String username);
    List<User> list(User filter);
    void save(User user);
    void remove(String id);
}
