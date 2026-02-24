package cn.wubo.sql.forge;

import cn.wubo.sql.forge.record.IBeforeRecordExecutor;
import cn.wubo.sql.forge.record.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class TimeUpdateExecute implements IBeforeRecordExecutor<Update> {
    @Override
    public Boolean support(String tableName, Update update) {
        return true;
    }

    @Override
    public Update before(String tableName, Update update) {
        if (update.sets().keySet().stream().noneMatch("update"::equalsIgnoreCase)) {
            Map<String, Object> newSets = update.sets();
            newSets.put("update", LocalDateTime.now());
            return new Update(newSets, update.wheres(), update.select());
        }
        return update;
    }
}
