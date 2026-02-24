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
import org.springframework.test.context.ActiveProfiles;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class BeforeRecordExcutorDatabaseTest {

    @Autowired
    RecordExecutor recordExecutor;

    @Test
    void testU() throws SQLException {
        String id = UUID.randomUUID().toString();
        List<Where> wheres = new ArrayList<>() {{
            add(new Where("id", ConditionType.EQ, id));
        }};
        Insert insert = new Insert(
                new HashMap<>() {{
                    put("id", id);
                    put("username", "BeforeRecordExcutorDatabaseTest");
                    put("email", "BeforeRecordExcutorDatabaseTest");
                }},
                new Select(
                        null,
                        wheres,
                        null,
                        null,
                        null,
                        false
                )
        );

        List<RowMap> rowMapList = (List<RowMap>) recordExecutor.insert("users", insert);
        log.info("rowMapList: {}", rowMapList);
        assertEquals(1, rowMapList.size());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        assertEquals(sdf.format(new Date()),sdf.format(rowMapList.get(0).get("CREATE")));
    }
}
