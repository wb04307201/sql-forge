package cn.wubo.sql.forge;

import lombok.Data;

/**
 * 角色-模板关联实体，表示角色与 Amis 模板之间的绑定关系。
 */
@Data
public class RoleTemplate {
    private String roleId;
    private String templateId;
}
