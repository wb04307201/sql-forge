package cn.wubo.sql.forge;

import java.util.List;

public interface IRoleTemplateStorage {
    List<String> listTemplateIdsByRole(String roleId);
    void save(RoleTemplate roleTemplate);
    void remove(String roleId, String templateId);
    void removeAllByRole(String roleId);
}
