package cn.wubo.sql.forge;

import java.util.List;

/**
 * 用户存储接口，定义用户的查找、列表、保存和删除操作。实现此接口可替换默认的内存存储。
 */
public interface IUserStorage {
    User findByUsername(String username);
    List<User> list(User filter);
    void save(User user);
    void remove(String id);
}
