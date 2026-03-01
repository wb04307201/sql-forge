package cn.wubo.sql.forge;

import cn.wubo.sql.forge.record.IBeforeRecordExecutor;
import cn.wubo.sql.forge.record.Update;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogUpdateExecute implements IBeforeRecordExecutor<Update> {
    @Override
    public Boolean support(String tableName, Update update) {
        return true;
    }

    @Override
    public Update before(String tableName, Update update) {
        log.info("LogUpdateExecute tableName: {} record: {}", tableName, update);
        return update;
    }
}
