package cn.wubo.sql.forge;

import cn.wubo.sql.forge.record.IBeforeRecordExecutor;
import cn.wubo.sql.forge.record.Insert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogInsertExecute implements IBeforeRecordExecutor<Insert> {
    @Override
    public Boolean support(String tableName, Insert insert) {
        return true;
    }

    @Override
    public Insert before(String tableName, Insert insert) {
        log.info("LogInsertExecute tableName: {} record: {}", tableName, insert);
        return insert;
    }
}
