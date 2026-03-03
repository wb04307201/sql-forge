package cn.wubo.sql.forge;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest()
@ActiveProfiles("test")
public class ChatTest {

    @Test
    public void test() {
        System.out.println("test");
    }
}
