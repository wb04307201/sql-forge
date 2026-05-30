package cn.wubo.sql.forge;

import lombok.Data;

/**
 * 用户-角色关联实体，表示用户与角色之间的绑定关系。
 */
@Data
public class UserRole {
    private String userId;
    private String roleId;
}
