package cn.wubo.sql.forge;

import cn.wubo.sql.forge.record.IBeforeRecordExecutor;
import cn.wubo.sql.forge.record.SelectPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogSelectPageExecute implements IBeforeRecordExecutor<SelectPage> {
    @Override
    public Boolean support(String tableName, SelectPage selectPage) {
        return true;
    }

    @Override
    public SelectPage before(String tableName, SelectPage selectPage) {
        log.info("LogSelectPageExecute tableName: {} record: {}", tableName, selectPage);
        return selectPage;
    }
}
