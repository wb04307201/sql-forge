package cn.wubo.sql.forge;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sql_forge_role")
public class Role {
    @Id private String id;
    @Column private String name;
    @Column private String description;
}
