package cn.wubo.sql.forge;

import java.util.List;

/**
 * 用户-角色关联存储接口，管理用户与角色的绑定关系。
 */
public interface IUserRoleStorage {
    List<String> listRoleIdsByUser(String userId);
    void save(UserRole userRole);
    void remove(String userId, String roleId);
    void removeAllByUser(String userId);
}
