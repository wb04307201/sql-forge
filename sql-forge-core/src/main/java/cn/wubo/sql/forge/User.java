package cn.wubo.sql.forge;

import lombok.Data;

/**
 * 用户实体，表示 sql-forge 系统的登录用户基本信息。
 */
@Data
public class User {
    private String id;
    private String username;
    private String password;
    private Boolean enabled;
    private String category;
}
