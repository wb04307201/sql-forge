package cn.wubo.sql.forge;

import java.util.List;

/**
 * 角色-模板关联存储接口，管理角色与 Amis 模板的绑定关系。
 */
public interface IRoleTemplateStorage {
    List<String> listTemplateIdsByRole(String roleId);
    void save(RoleTemplate roleTemplate);
    void remove(String roleId, String templateId);
    void removeAllByRole(String roleId);
}
