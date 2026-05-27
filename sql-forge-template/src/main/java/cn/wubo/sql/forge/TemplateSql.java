package cn.wubo.sql.forge;

import lombok.Data;

@Data
public class TemplateSql {
    private String id;
    private String name;
    private String description;
    private String executorName;
    private String context;
}
