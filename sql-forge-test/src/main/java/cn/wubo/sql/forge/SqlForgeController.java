package cn.wubo.sql.forge;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/sql/forge")
public class SqlForgeController {

    private final ChatClient chatClient;

    private final SqlForgeProperties sqlForgeProperties;

    public SqlForgeController(ChatClient chatClient, SqlForgeProperties sqlForgeProperties) {
        this.chatClient = chatClient;
        this.sqlForgeProperties = sqlForgeProperties;
    }

    @PostMapping(value = "/ai", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAi(@RequestBody AiRequest aiRequest) {
        // 1. 显式设置超时时间（单位毫秒），0 表示永不超时
        SseEmitter emitter = new SseEmitter(0L);

        // 2. 设置超时回调，防止连接泄露
        emitter.onTimeout(() -> {
            System.out.println("SSE 连接超时");
            emitter.complete();
        });
        emitter.onCompletion(() -> System.out.println("SSE 连接完成"));
        emitter.onError(e -> System.out.println("SSE 连接错误：" + e.getMessage()));

        // 3. 在异步线程中处理 AI 请求，避免阻塞 Tomcat 线程
        CompletableFuture.runAsync(() -> {
            try {
                String template = prepareTemplate(aiRequest);

                // 4. 订阅 Flux 流并将数据发送给 emitter
                chatClient.prompt()
                        .user(template)
                        .stream()
                        .content()
                        .subscribe(
                                content -> {
                                    try {
                                        emitter.send(content, MediaType.TEXT_PLAIN);
                                    } catch (IOException e) {
                                        emitter.completeWithError(e);
                                    }
                                },
                                error -> emitter.completeWithError(error),
                                () -> emitter.complete()
                        );
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private String prepareTemplate(AiRequest aiRequest) {
        // 你的模板替换逻辑
        String template = sqlForgeProperties.getAi().getPromptTemplate();
        template = template.replace("{{API_SPEC}}", sqlForgeProperties.getAi().getApiSpec());
        template = template.replace("{{TABLE_INFO}}", aiRequest.tableInfo());
        template = template.replace("{{EXAMPLE_TABLE_INFO}}", sqlForgeProperties.getAi().getExampleTableInfo());
        template = template.replace("{{EXAMPLE_AMIS_INFO}}", sqlForgeProperties.getAi().getExampleAmisInfo());
        return template;
    }
}
