package cn.wubo.sql.forge;

import cn.wubo.sql.forge.map.ParamMap;
import cn.wubo.sql.forge.map.RowMap;
import cn.wubo.sql.forge.records.SqlScript;
import com.mysql.cj.jdbc.MysqlDataSource;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class ExecutorServiceCalciteTest {

    @Autowired
    ExecutorService executorService;

    @BeforeAll
    static void setUp() {
        CalciteHelp.init();
    }

    @Test
    void testExcutorName() {
        IExecutor executor = executorService.getExecutor("calcite");
        assertEquals("calcite", executor.getExecutorName());
    }

    @Test
    void testMetaData() throws SQLException {
        TreeNode<?> treeNode = executorService.getExecutor("calcite").getMetaData();
        assertEquals("Calcite", treeNode.getValue());
    }

    @Test
    void testSelect() throws SQLException {
        IExecutor executor = executorService.getExecutor("calcite");

        ParamMap params = new ParamMap();
        params.put(0);

        SqlScript sqlScript = new SqlScript("""
                select student.name, sum(score.grade) as grade 
                from MYSQL.student as student join POSTGRES.score as score on student.id=score.student_id 
                where student.id > ? 
                group by student.name
                """, params);

        List<RowMap> rowMapList = executor.executeQuery(sqlScript);
        log.info("rowMapList: {}", rowMapList);
        assertEquals(7, rowMapList.size());
    }
}
