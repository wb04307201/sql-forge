package cn.wubo.sql.forge;

import java.util.List;

/**
 * 用户-角色关联存储接口，管理用户与角色的绑定关系。
 */
public interface IUserRoleStorage {

    /**
     * 查询指定用户拥有的角色 ID 列表。
     *
     * @param userId 用户 ID
     * @return 角色 ID 列表
     */
    List<String> listRoleIdsByUser(String userId);

    /**
     * 保存用户-角色关联。
     *
     * @param userRole 用户-角色关联
     */
    void save(UserRole userRole);

    /**
     * 删除指定的用户-角色关联。
     *
     * @param userId 用户 ID
     * @param roleId 角色 ID
     */
    void remove(String userId, String roleId);

    /**
     * 删除指定用户的所有角色关联。
     *
     * @param userId 用户 ID
     */
    void removeAllByUser(String userId);
}
