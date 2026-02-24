package cn.wubo.sql.forge;

import cn.wubo.sql.forge.records.SqlScript;

import java.sql.SQLException;
import java.util.Map;

public record TemplateSqlExcutor(
        ITemplateSqlStorage templateStorage,
        ExecutorService executorService
) {

    public Object execute(String id, Map<String, Object> params) throws SQLException {
        TemplateSql template = templateStorage.get(id);
        IExecutor executor = executorService.getExecutor(template.getExecutorName());
        TemplateSqlEngine engine = new TemplateSqlEngine();
        SqlScript result = engine.process(template.getContext(), params);
        return executor.execute(result);
    }

}
