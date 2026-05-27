package cn.wubo.sql.forge;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sql_forge_role_template")
public class RoleTemplate {
    @Id @Column(name = "role_id") private String roleId;
    @Id @Column(name = "template_id") private String templateId;
}
