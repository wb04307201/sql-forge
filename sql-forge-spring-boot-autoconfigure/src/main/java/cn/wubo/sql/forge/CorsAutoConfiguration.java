package cn.wubo.sql.forge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SQL Forge API 跨域配置。
 * <p>
 * 默认启用 CORS（{@code sql.forge.cors.enabled=true}），允许浏览器从任意 origin
 * 通过 XHR/fetch 调用 {@code /sql/forge/api/**}。常用于：
 * </p>
 * <ul>
 *   <li>amis / 低代码页面直接通过浏览器跨域调用 SQL Forge API</li>
 *   <li>sql-forge-mcp 的 PlaywrightRenderer 预览（about:blank 加载的页面需要 CORS 才能拉数据）</li>
 * </ul>
 * <p>
 * 生产环境建议改成具体 origin 白名单（{@code sql.forge.cors.allowed-origins=https://your.app}）。
 * </p>
 */
@Configuration
@ConditionalOnProperty(prefix = "sql.forge.cors", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CorsAutoConfiguration {

    /**
     * 默认 CORS 配置：允许所有 origin、常见方法、所有头。
     */
    @Bean
    @ConditionalOnMissingBean(name = "sqlForgeCorsConfigurer")
    public WebMvcConfigurer sqlForgeCorsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/sql/forge/api/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}
