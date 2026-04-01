package cn.wubo.sql.forge.mcp.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient 配置类
 * 
 * 手动配置 ChatClient Bean，确保可以在整个应用中通过依赖注入使用
 */
@Configuration
public class ChatClientConfig {

    /**
     * 配置 ChatClient Bean
     * 可以在应用的任何地方通过 @Autowired 注入使用
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                // 可以在这里添加默认配置
                // .defaultSystem("你是一个SQL专家助手")
                // .defaultOptions(ChatOptions.builder().temperature(0.3).build())
                .build();
    }
    
    /**
     * 配置 ChatClient.Builder Bean（如果需要）
     */
    @Bean
    public ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
        return ChatClient.builder(chatModel);
    }
}
