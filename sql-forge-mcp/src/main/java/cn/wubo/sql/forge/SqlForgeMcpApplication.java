package cn.wubo.sql.forge;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@EnableConfigurationProperties({SqlForgeMcpProperties.class})
public class SqlForgeMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlForgeMcpApplication.class, args);
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .build();
    }

    @Bean
    public SqlForgeMcpService sqlForgeMcpService(SqlForgeMcpProperties sqlForgeMcpProperties, RestClient restClient) {
        return new SqlForgeMcpService(sqlForgeMcpProperties.getSystems(), restClient);
    }

    @Bean
    public ToolCallbackProvider sqlForgeMcpTools(SqlForgeMcpService sqlForgeMcpService) {
        return MethodToolCallbackProvider.builder().toolObjects(sqlForgeMcpService).build();
    }
}
