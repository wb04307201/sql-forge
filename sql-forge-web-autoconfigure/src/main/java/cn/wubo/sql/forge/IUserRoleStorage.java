package cn.wubo.sql.forge;

import java.util.List;

public interface IUserRoleStorage {
    List<String> listRoleIdsByUser(String userId);
    void save(UserRole userRole);
    void remove(String userId, String roleId);
    void removeAllByUser(String userId);
}
