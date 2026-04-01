package cn.wubo.sql.forge.mcp;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

@SpringBootApplication
public class SqlForgeMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlForgeMcpApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider nl2sqlTools(@Lazy NL2SQLService nl2SQLService) {
        return MethodToolCallbackProvider.builder().toolObjects(nl2SQLService).build();
    }

    @ConditionalOnMissingBean(VectorStore.class)
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
        builder.defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(), // chat-memory advisor
//                ToolSearchToolCallAdvisor.builder().toolSearcher(toolSearcher).build(), // tool-search advisor
                new SimpleLoggerAdvisor() // logger advisor
        );
        return builder.build();
    }

}
