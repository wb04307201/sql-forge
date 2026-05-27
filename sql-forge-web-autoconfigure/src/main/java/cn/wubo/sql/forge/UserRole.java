package cn.wubo.sql.forge;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sql_forge_user_role")
public class UserRole {
    @Id @Column(name = "user_id") private String userId;
    @Id @Column(name = "role_id") private String roleId;
}
