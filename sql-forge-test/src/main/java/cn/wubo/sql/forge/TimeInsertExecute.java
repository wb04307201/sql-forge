package cn.wubo.sql.forge;

import cn.wubo.sql.forge.record.IBeforeRecordExecutor;
import cn.wubo.sql.forge.record.Insert;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class TimeInsertExecute implements IBeforeRecordExecutor<Insert> {
    @Override
    public Boolean support(String tableName, Insert insert) {
        return true;
    }

    @Override
    public Insert before(String tableName, Insert insert) {
        if (insert.sets().keySet().stream().noneMatch("create"::equalsIgnoreCase)){
            Map<String, Object> newSets = insert.sets();
            newSets.put("create", LocalDateTime.now());
            return new Insert(newSets, insert.select());
        }

        return null;
    }
}
