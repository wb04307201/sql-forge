package cn.wubo.sql.forge;

import cn.wubo.sql.forge.record.IBeforeRecordExecutor;
import cn.wubo.sql.forge.record.Select;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogSelectExecute implements IBeforeRecordExecutor<Select> {
    @Override
    public Boolean support(String tableName, Select select) {
        return true;
    }

    @Override
    public Select before(String tableName, Select select) {
        log.info("LogSelectExecute tableName: {} record: {}", tableName, select);
        return select;
    }
}
