package cn.wubo.sql.forge;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sql_forge_template_sql")
public class SqlForgeTemplateSql {

    @Id
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "executor_name")
    private String executorName;

    @Column(name = "context")
    private String context;
}
