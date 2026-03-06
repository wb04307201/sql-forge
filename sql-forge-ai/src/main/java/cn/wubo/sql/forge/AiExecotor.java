package cn.wubo.sql.forge;

import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

public record AiExecotor(
        ChatClient chatClient,
        SqlForgeProperties.AiProperties aiProperties
) {

    public Flux<String> execute(AiRequest aiRequest) {
        SqlForgeProperties sqlForgeProperties = new SqlForgeProperties();
        String template = sqlForgeProperties.getAi().getPromptTemplate();
        template = template.replace("{{API_SPEC}}", sqlForgeProperties.getAi().getApiSpec());
        template = template.replace("{{TABLE_INFO}}", aiRequest.tableInfo());
        template = template.replace("{{EXAMPLE_TABLE_INFO}}", sqlForgeProperties.getAi().getExampleTableInfo());
        template = template.replace("{{EXAMPLE_AMIS_INFO}}", sqlForgeProperties.getAi().getExampleAmisInfo());

        return chatClient.prompt().user(template).stream().content();
    }
}
