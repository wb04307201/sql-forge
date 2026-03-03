package cn.wubo.sql.forge;

import cn.wubo.sql.forge.enums.ConditionType;
import cn.wubo.sql.forge.enums.JoinType;
import cn.wubo.sql.forge.record.*;
import cn.wubo.sql.forge.record.base.Join;
import cn.wubo.sql.forge.record.base.Page;
import cn.wubo.sql.forge.record.base.Where;
import lombok.extern.slf4j.Slf4j;
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
public class ApiJsonDatabaseTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void testSelect() {
        Select select = new Select(
                List.of(
                        "orders.id AS order_id",
                        "users.username",
                        "products.name AS product_name",
                        "products.price",
                        "orders.quantity",
                        "(products.price * orders.quantity) AS total"
                ),
                null,
                new ArrayList<>() {{
                    add(new Join(JoinType.INNER_JOIN, "users","orders.user_id = users.id"));
                    add(new Join(JoinType.INNER_JOIN, "products","orders.product_id = products.id"));
                }},
                null,
                null,
                false
        );


        String baseUrl = "http://localhost:" + port;
        ResponseEntity<List> response = restTemplate.postForEntity(
                baseUrl + "/sql/forge/api/json/select/orders",
                select,
                List.class
        );

        assertNotEquals(null, response.getBody());
        assertEquals(4, response.getBody().size());
    }

    @Test
    void testSelectPage() {
        SelectPage select = new SelectPage(
                List.of(
                        "orders.id AS order_id",
                        "users.username",
                        "products.name AS product_name",
                        "products.price",
                        "orders.quantity",
                        "(products.price * orders.quantity) AS total"
                ),
                null,
                new Page(0,2),
                new ArrayList<>() {{
                    add(new Join(JoinType.INNER_JOIN, "users","orders.user_id = users.id"));
                    add(new Join(JoinType.INNER_JOIN, "products","orders.product_id = products.id"));
                }},
                null,
                false
        );


        String baseUrl = "http://localhost:" + port;
        ResponseEntity<SelectPageResult> response = restTemplate.postForEntity(
                baseUrl + "/sql/forge/api/json/selectPage/orders",
                select,
                SelectPageResult.class
        );

        assertNotEquals(null, response.getBody());
        assertEquals(2, response.getBody().rows().size());
    }

    @Test
    void test() {
        String id = UUID.randomUUID().toString();
        List<Where> wheres = new ArrayList<>() {{
            add(new Where("id", ConditionType.EQ, id));
        }};
        Insert insert = new Insert(
                new HashMap<>() {{
                    put("id", id);
                    put("username", "wb04307201");
                    put("email", "wb04307201@gitee.com");
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

        String baseUrl = "http://localhost:" + port;
        ResponseEntity<List> response = restTemplate.postForEntity(
                baseUrl + "/sql/forge/api/json/insert/users",
                insert,
                List.class
        );

        assertEquals(1, response.getBody().size());
        assertEquals(id, ((Map<String, Object>)response.getBody().get(0)).get("ID"));
        assertEquals("wb04307201@gitee.com", ((Map<String, Object>)response.getBody().get(0)).get("EMAIL"));

        Update update = new Update(
                new HashMap<>() {{
                    put("email", "wb04307201@github.com");
                }},
                wheres,
                new Select(
                        null,
                        wheres,
                        null,
                        null,
                        null,
                        false
                )
        );

        response = restTemplate.postForEntity(
                baseUrl + "/sql/forge/api/json/update/users",
                update,
                List.class
        );

        assertEquals(1, response.getBody().size());
        assertEquals(id, ((Map<String, Object>)response.getBody().get(0)).get("ID"));
        assertEquals("wb04307201@github.com", ((Map<String, Object>)response.getBody().get(0)).get("EMAIL"));

        Delete delete = new Delete(
                wheres,
                new Select(
                        null,
                        wheres,
                        null,
                        null,
                        null,
                        false
                )
        );

        response = restTemplate.postForEntity(
                baseUrl + "/sql/forge/api/json/delete/users",
                delete,
                List.class
        );

        assertEquals(0, response.getBody().size());
    }
}
