package cn.wubo.sql.forge.mcp.config;

import cn.wubo.sql.forge.mcp.service.GenerateJsonService;
import cn.wubo.sql.forge.mcp.service.NL2SQLService;
import cn.wubo.sql.forge.mcp.service.TestService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * @author wangyong
 * @version 1.0
 * @description ToolCallbackProviderConfig
 * @date 2026-04-01 20:31:07
 */
@Configuration
public class ToolCallbackProviderConfig {

    @Bean
    public ToolCallbackProvider sqlForgeTools(@Lazy NL2SQLService nl2SQLService, @Lazy GenerateJsonService generateJsonService, @Lazy TestService testService) {
        return MethodToolCallbackProvider.builder().toolObjects(nl2SQLService, generateJsonService, testService).build();
    }
}
