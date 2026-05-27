package cn.wubo.sql.forge;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "users")
public class User {
    @Id private String id;
    @Column private String username;
    @Column private String password;
    @Column private Boolean enabled;
    @Column private String category;  // "admin" | "user" — 用户分类，非角色
    @Column(name = "dict_sex") private String dictSex;
    @Column private String email;
    @Column(name = "created_time") private String createdTime;
    @Column(name = "updated_time") private String updatedTime;
}
