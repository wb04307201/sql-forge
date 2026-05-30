package cn.wubo.sql.forge;

import lombok.Data;

/**
 * 角色实体，表示系统角色信息。
 */
@Data
public class Role {
    private String id;
    private String name;
    private String description;
}
