package cn.wubo.sql.forge;

import java.util.List;

/**
 * 用户存储接口，定义用户的查找、列表、保存和删除操作。实现此接口可替换默认的内存存储。
 */
public interface IUserStorage {

    /**
     * 根据用户名查找用户。
     *
     * @param username 用户名
     * @return 匹配的用户，不存在时返回 null
     */
    User findByUsername(String username);

    /**
     * 按条件查询用户列表。
     *
     * @param filter 过滤条件
     * @return 符合条件的用户列表
     */
    List<User> list(User filter);

    /**
     * 保存用户，若 ID 为空则自动生成。
     *
     * @param user 用户信息
     */
    void save(User user);

    /**
     * 根据 ID 删除用户。
     *
     * @param id 用户 ID
     */
    void remove(String id);
}
