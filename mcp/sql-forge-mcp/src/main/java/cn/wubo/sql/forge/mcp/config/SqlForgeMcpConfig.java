package cn.wubo.sql.forge.mcp.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wangyong
 * @version 1.0
 * @description SqlForgeMcpConfig
 * @date 2026-04-01 20:14:23
 */
@Configuration
public class SqlForgeMcpConfig {
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
        builder.defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                new SimpleLoggerAdvisor()
        );
        return builder.build();
    }

}
