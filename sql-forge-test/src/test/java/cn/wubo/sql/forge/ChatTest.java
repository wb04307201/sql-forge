package cn.wubo.sql.forge;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@SpringBootTest()
@ActiveProfiles("test")
class ChatTest {

    @Autowired
    ChatClient chatClient;

    @Test
    void call() {
        String result = chatClient.prompt().user("你好!").call().content();
        log.info("result: {}", result);
    }

    @Test
    void stream() throws InterruptedException {
        Flux<String> flux = chatClient.prompt().user("你好!").stream().content();
        flux.subscribe(log::info);
        Thread.sleep(5000);
    }
}
