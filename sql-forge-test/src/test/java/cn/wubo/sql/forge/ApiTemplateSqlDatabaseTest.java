package cn.wubo.sql.forge;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ApiTemplateSqlDatabaseTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void test() {
        TemplateSql template = new TemplateSql();
        template.setId("ApiTemplateDatabaseTest");
        template.setExecutorName("database");
        template.setContext("""
SELECT * FROM users WHERE 1=1
<if test="name != null && name != ''"> AND username = #{name}</if>
<if test="ids != null && !ids.isEmpty()"><foreach collection="ids" item="id" open=" AND id IN (" separator="," close=")">#{id}</foreach></if>
<if test="(name == null || name == '') && (ids == null || ids.isEmpty()) "> AND 0=1</if>
""");

        String baseUrl = "http://localhost:" + port;

        // 创建带请求头的PUT请求
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("X-Request-Source", "test");

        HttpEntity<TemplateSql> requestPutEntity = new HttpEntity<>(template, headers);

        ResponseEntity<Boolean> responsePut = restTemplate.exchange(
                baseUrl + "/sql/forge/api/template/sql",
                HttpMethod.PUT,
                requestPutEntity,
                Boolean.class
        );

        assertEquals(Boolean.TRUE, responsePut.getBody());

        ResponseEntity<List> responseExecute = restTemplate.postForEntity(
                baseUrl + "/sql/forge/api/template/sql/ApiTemplateDatabaseTest",
                Map.of("name", "alice", "ids", List.of(
                        "550e8400-e29b-41d4-a716-446655440000",
                        "550e8400-e29b-41d4-a716-446655440001",
                        "550e8400-e29b-41d4-a716-446655440002"
                )),
                List.class
        );

        assertNotEquals(null, responseExecute.getBody());
        assertEquals(1, responseExecute.getBody().size());
    }
}
