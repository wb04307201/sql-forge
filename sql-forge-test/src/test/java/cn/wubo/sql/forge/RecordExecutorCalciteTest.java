package cn.wubo.sql.forge;

import cn.wubo.sql.forge.record.base.Join;
import cn.wubo.sql.forge.record.base.Where;
import cn.wubo.sql.forge.enums.ConditionType;
import cn.wubo.sql.forge.enums.JoinType;
import cn.wubo.sql.forge.map.RowMap;
import cn.wubo.sql.forge.record.Select;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest()
@ActiveProfiles("test")
@ExtendWith(CalciteCondition.class)
public class RecordExecutorCalciteTest {

    @Autowired
    RecordExecutor recordExecutor;

    @BeforeAll
    static void setUp() {
        CalciteHelp.init();
    }

    @Test
    void testSelect() throws SQLException {
        Select select = new Select(
                List.of(
                        "student.name",
                        "sum(score.grade) as grade"
                ),
                new ArrayList<>(){{
                    add(new Where("student.id", ConditionType.GT, 0));
                }},
                new ArrayList<>() {{
                    add(new Join(JoinType.JOIN, "POSTGRES.score as score", "student.id=score.student_id"));
                }},
                null,
                new ArrayList<>(){{
                    add("student.name");
                }},
                false
        );

        List<RowMap> rowMapList = recordExecutor.select("calcite", "MYSQL.student as student", select);
        log.info("rowMapList: {}", rowMapList);
        assertEquals(7, rowMapList.size());
    }

}
