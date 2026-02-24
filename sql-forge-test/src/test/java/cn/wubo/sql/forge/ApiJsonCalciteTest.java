package cn.wubo.sql.forge;

import cn.wubo.sql.forge.enums.ConditionType;
import cn.wubo.sql.forge.enums.JoinType;
import cn.wubo.sql.forge.map.RowMap;
import cn.wubo.sql.forge.record.*;
import cn.wubo.sql.forge.record.base.Join;
import cn.wubo.sql.forge.record.base.Page;
import cn.wubo.sql.forge.record.base.Where;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ApiJsonCalciteTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @BeforeAll
    static void setUp() {
        CalciteHelp.init();
    }

    @LocalServerPort
    private int port;

    @Test
    void testSelect() {
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


        String baseUrl = "http://localhost:" + port;
        ResponseEntity<List> response = restTemplate.postForEntity(
                baseUrl + "/sql/forge/api/json/select/MYSQL.student as student?executorName=calcite",
                select,
                List.class
        );

        assertNotEquals(null, response.getBody());
        assertEquals(7, response.getBody().size());
    }
}
