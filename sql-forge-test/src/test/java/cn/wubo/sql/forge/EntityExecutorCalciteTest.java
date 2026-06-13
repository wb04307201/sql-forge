package cn.wubo.sql.forge;

import cn.wubo.sql.forge.Entity;
import cn.wubo.sql.forge.EntityExecutor;
import cn.wubo.sql.forge.entity.*;
import cn.wubo.sql.forge.record.SelectPageResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(CalciteCondition.class)
public class EntityExecutorCalciteTest {

    @Autowired
    EntityExecutor entityExecutor;

    @BeforeAll
    static void setUp() {
        CalciteHelp.init();
    }

    @Test
    void testEntitySelect() throws Exception {
        EntitySelect<Student> select = Entity.select(Student.class);
        List<Student> students = entityExecutor.run("calcite",select);
        log.info("{}", students);
        assertEquals(7, students.size());
    }
}
