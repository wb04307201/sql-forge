package cn.wubo.sql.forge;

import cn.wubo.sql.forge.record.Delete;
import cn.wubo.sql.forge.record.IBeforeRecordExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogDeleteExecute implements IBeforeRecordExecutor<Delete> {
    @Override
    public Boolean support(String tableName, Delete delete) {
        return true;
    }

    @Override
    public Delete before(String tableName, Delete delete) {
        log.info("LogDeleteExecute tableName: {} record: {}", tableName, delete);
        return delete;
    }
}
