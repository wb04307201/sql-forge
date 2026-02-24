package cn.wubo.sql.forge;

import cn.wubo.sql.forge.map.RowMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class TemplateSqlExcutorCalciteTest {

    @Autowired
    ITemplateSqlStorage templateStorage;

    @Autowired
    TemplateSqlExcutor templateExcutor;

    @Test
    void test() throws Exception {
        TemplateSql template = new TemplateSql();
        template.setId("TemplateExcutorCalciteTest");
        template.setExecutorName("calcite");
        template.setContext("""
select student.name, sum(score.grade) as grade
from MYSQL.student as student join POSTGRES.score as score on student.id=score.student_id where 1=1
<if test="ids == null || ids.isEmpty()">AND 0=1</if>
<if test="ids != null && !ids.isEmpty()">
<foreach collection="ids" item="id" open="AND student.id IN (" separator="," close=")">#{id}</foreach>
</if>
group by student.name
""");

        templateStorage.save(template);

        List<RowMap> rowMaps = (List<RowMap>) templateExcutor.execute("TemplateExcutorCalciteTest", Map.of("ids", List.of(1, 2, 3, 4, 5, 6, 7)));
        log.info("{}", rowMaps);
        assertEquals(7, rowMaps.size());
    }
}
