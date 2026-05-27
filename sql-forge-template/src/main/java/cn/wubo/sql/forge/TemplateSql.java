package cn.wubo.sql.forge;

import lombok.Data;

/**
 * SQL 模板数据模型，包含模板 ID、名称、描述、执行器名称和模板内容。
 */
@Data
public class TemplateSql {
    private String id;
    private String name;
    private String description;
    private String executorName;
    private String context;
}
