package cn.wubo.sql.forge;

import cn.wubo.sql.forge.map.ParamMap;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransactionalComponent {

    private final ExecutorService executorService;

    public TransactionalComponent(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Transactional
    void insert() throws SQLException {
        IExecutor executor = executorService.getExecutor("database");

        ParamMap params = new ParamMap();
        params.put("1");
        params.put("wb04307201");
        params.put("wb04307201@gitee.com");

        SqlScript sqlScript = new SqlScript("""
                INSERT INTO users (id,username,email) VALUES (?,?,?)
                """, params);

        Object key = executor.executeInsert(sqlScript);
        assertEquals("1",((Map<String,String>)key).get("ID"));
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }
}
