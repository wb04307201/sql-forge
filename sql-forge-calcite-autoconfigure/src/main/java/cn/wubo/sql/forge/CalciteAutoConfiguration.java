package cn.wubo.sql.forge;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@AutoConfiguration
@EnableConfigurationProperties(SqlForgeProperties.class)
public class CalciteAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "sql.forge.calcite.enabled", havingValue = "true")
    public IExecutor calciteExcutor(ResourceLoader resourceLoader, SqlForgeProperties properties) throws IOException {
        if (!StringUtils.hasText(properties.getCalcite().getConfiguration())) {
            throw new IllegalArgumentException("sql.forge.api.calcite.configuration is null");
        }

        String configPath = properties.getCalcite().getConfiguration();
        Resource resource = resourceLoader.getResource(configPath);

        if (!resource.exists()) {
            throw new FileNotFoundException("Calcite配置文件不存在: " + configPath);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> modelMap = mapper.readValue(inputStream, new com.fasterxml.jackson.core.type.TypeReference<>() {
            });
            String model = mapper.writeValueAsString(modelMap);
            return new CalciteExcutor(model, properties);
        }
    }
}
