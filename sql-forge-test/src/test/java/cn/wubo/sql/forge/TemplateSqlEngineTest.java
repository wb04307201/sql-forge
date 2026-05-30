package cn.wubo.sql.forge;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Slf4j
public class TemplateSqlEngineTest {

    @Test
    void testWitchPlaceholders() {

        String template = "SELECT * FROM users WHERE 1=1" +
                "<if test=\"name != null && name != ''\"> AND username = #{name}</if>" +
                "<if test=\"ids != null && !ids.isEmpty()\"><foreach collection=\"ids\" item=\"id\" open=\" AND id IN (\" separator=\",\" close=\")\">#{id}</foreach></if>" +
                "<if test=\"(name == null || name == '') && (ids == null || ids.isEmpty()) \"> AND 0=1</if>" +
                " ORDER BY username DESC";

        Map<String, Object> input = new HashMap<>();
        input.put("name", "John");
        input.put("ids", Arrays.asList("550e8400-e29b-41d4-a716-446655440000", "550e8400-e29b-41d4-a716-446655440001", "550e8400-e29b-41d4-a716-446655440002"));

        TemplateSqlEngine engine = new TemplateSqlEngine();
        SqlScript result = engine.process(template, input);

        assertEquals("SELECT * FROM users WHERE 1=1 AND username = ? AND id IN (?,?,?) ORDER BY username DESC", result.sql());
        assertEquals("{1=John, 2=550e8400-e29b-41d4-a716-446655440000, 3=550e8400-e29b-41d4-a716-446655440001, 4=550e8400-e29b-41d4-a716-446655440002}", result.params().toString());

        input.clear();
        input.put("name", null);
        input.put("ids", null);
        SqlScript result2 = engine.process(template, input);
        assertEquals("SELECT * FROM users WHERE 1=1 AND 0=1 ORDER BY username DESC", result2.sql());
        assertEquals("{}", result2.params().toString());


        input.clear();
        input.put("name", "");
        input.put("ids", Collections.emptyList());
        SqlScript result3 = engine.process(template, input);
        assertEquals("SELECT * FROM users WHERE 1=1 AND 0=1 ORDER BY username DESC", result3.sql());
        assertEquals("{}", result3.params().toString());
    }

    @Test
    void testWithValues() {
        String template = "SELECT * FROM users WHERE 1=1" +
                "<if test=\"name != null && name != ''\"> AND username = #{name}</if>" +
                "<if test=\"ids != null && !ids.isEmpty()\"><foreach collection=\"ids\" item=\"id\" open=\" AND id IN (\" separator=\",\" close=\")\">#{id}</foreach></if>" +
                "<if test=\"(name == null || name == '') && (ids == null || ids.isEmpty()) \"> AND 0=1</if>" +
                " ORDER BY username DESC";

        Map<String, Object> input = new HashMap<>();
        input.put("name", "John");
        input.put("ids", Arrays.asList("550e8400-e29b-41d4-a716-446655440000", "550e8400-e29b-41d4-a716-446655440001", "550e8400-e29b-41d4-a716-446655440002"));

        TemplateSqlEngine engine = new TemplateSqlEngine();
        SqlScript result = engine.process(template, input, GenerationSqlMode.WITH_VALUES);

        assertEquals("SELECT * FROM users WHERE 1=1 AND username = 'John' AND id IN ('550e8400-e29b-41d4-a716-446655440000','550e8400-e29b-41d4-a716-446655440001','550e8400-e29b-41d4-a716-446655440002') ORDER BY username DESC", result.sql());
        assertEquals("{}", result.params().toString());
    }
}
