package cn.wubo.sql.forge.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SqlForgeMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlForgeMcpApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider weatherTools(NL2SQLService nl2SQLService) {
        return MethodToolCallbackProvider.builder().toolObjects(nl2SQLService).build();
    }
}
