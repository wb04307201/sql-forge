package cn.wubo.sql.forge;

import cn.wubo.sql.forge.amis.AmisKnowledgeService;
import cn.wubo.sql.forge.amis.AmisValidator;
import cn.wubo.sql.forge.amis.PlaywrightRenderer;
import com.microsoft.playwright.Playwright;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * sql-forge-mcp Spring Boot 启动类：组装 5 个领域 Service + 1 个 facade + 共享 Support。
 *
 * <p>架构（Round 5）：</p>
 * <pre>
 *   ┌─ McpToolSupport（共享：withCtx / encodePath / checkBodySize / 错误映射）
 *   │
 *   ├─ MetadataService   ── 8 个 Tool（getMetaData* / findTablesByName / describeSchema / countRows / listExecutorNames）
 *   ├─ JsonCrudService   ── 6 个 Tool（jsonSelect/Insert/Update/Delete/SelectPage + executeSQL）
 *   ├─ TemplateService   ── 10 个 Tool（list/get/save/delete Amis + SQL 模板 + executeSqlTemplate/Safely）
 *   ├─ AmisService       ── 2 个 Tool（validateAmisTemplate / previewAmisTemplate）
 *   └─ SqlForgeMcpService ── 2 个 Tool（mcpHealth / metrics）facade
 *
 *   Total: 28 个业务 Tool + 1 个 metrics = 29 个 @Tool 方法
 * </pre>
 */
@SpringBootApplication
@EnableConfigurationProperties({SqlForgeMcpProperties.class})
public class SqlForgeMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlForgeMcpApplication.class, args);
    }

    // ============ 基础设施 ============

    /**
     * 远端 REST 客户端：连接池 + 超时（Round 5 R5-3 升级为 httpclient5 连接池）。
     * <ul>
     *   <li>connectTimeout 3s — TCP 连接建立超过 3 秒即放弃</li>
     *   <li>readTimeout 10s — 单个 HTTP 请求超过 10 秒无响应即放弃</li>
     *   <li>连接池：maxTotal=50, perRoute=10 — 高并发场景下避免每次新建 socket</li>
     * </ul>
     * <p>
     * httpclient5 通过 {@code optional=true} 引入，缺失时回退到 {@code SimpleClientHttpRequestFactory}（无连接池）。
     * </p>
     */
    @Bean
    public RestClient restClient() {
        // 检测 httpclient5 是否在 classpath（optional=true 时可能不存在）
        if (isClassPresent("org.apache.hc.client5.http.classic.HttpClient")) {
            return pooledRestClient();
        }
        return simpleRestClient();
    }

    /**
     * 使用 httpclient5 连接池的 RestClient（高并发场景）。
     */
    private RestClient pooledRestClient() {
        org.apache.hc.client5.http.config.ConnectionConfig connectionConfig =
                org.apache.hc.client5.http.config.ConnectionConfig.custom()
                        .setConnectTimeout(3, TimeUnit.SECONDS)
                        .setSocketTimeout(10, TimeUnit.SECONDS)
                        .build();
        org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager connManager =
                org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder.create()
                        .setDefaultConnectionConfig(connectionConfig)
                        .setMaxConnTotal(50)
                        .setMaxConnPerRoute(10)
                        .build();
        org.apache.hc.client5.http.classic.HttpClient httpClient =
                org.apache.hc.client5.http.impl.classic.HttpClients.custom()
                        .setConnectionManager(connManager)
                        .disableAutomaticRetries()  // 让 RestClient 处理重试，避免双重
                        .build();
        org.springframework.http.client.HttpComponentsClientHttpRequestFactory factory =
                new org.springframework.http.client.HttpComponentsClientHttpRequestFactory(httpClient);
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    /**
     * 简单 RestClient（无连接池，回退方案，单测环境用）。
     */
    private RestClient simpleRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    /**
     * 检查 class 是否在 classpath（不抛 ClassNotFoundException）。
     */
    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, SqlForgeMcpApplication.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Bean
    public AmisKnowledgeService amisKnowledgeService() throws java.io.IOException {
        return new AmisKnowledgeService();
    }

    @Bean
    public MetricsService metricsService() {
        return new MetricsService();
    }

    @Bean
    public AuditLogger auditLogger() {
        return new AuditLogger();
    }

    @Bean
    public AmisValidator amisValidator(AmisKnowledgeService amisKnowledgeService) {
        return new AmisValidator(amisKnowledgeService);
    }

    @Bean
    @ConditionalOnClass(Playwright.class)
    public PlaywrightRenderer playWrightRenderer() {
        return new PlaywrightRenderer();
    }

    // ============ 共享 Support ============

    /**
     * 业务 Tool 共用辅助：系统上下文、错误映射、URL 编码、大小校验、指标 + 审计。
     */
    @Bean
    public McpToolSupport mcpToolSupport(SqlForgeMcpProperties properties,
                                         MetricsService metricsService,
                                         AuditLogger auditLogger) {
        return new McpToolSupport(properties.getSystems(),
                metricsService, auditLogger,
                properties.getMaxRequestBytes(),
                properties.getMaxResponseBytes());
    }

    // ============ 5 个领域 Service ============

    @Bean
    public MetadataService metadataService(McpToolSupport support, RestClient restClient) {
        return new MetadataService(support, restClient);
    }

    @Bean
    public JsonCrudService jsonCrudService(McpToolSupport support, RestClient restClient) {
        return new JsonCrudService(support, restClient);
    }

    @Bean
    public TemplateService templateService(McpToolSupport support, RestClient restClient) {
        return new TemplateService(support, restClient);
    }

    @Bean
    public AmisService amisService(McpToolSupport support,
                                   AmisValidator amisValidator,
                                   org.springframework.beans.factory.ObjectProvider<PlaywrightRenderer> playWrightRenderer) {
        return new AmisService(support, amisValidator, playWrightRenderer.getIfAvailable());
    }

    @Bean
    public SqlForgeMcpService sqlForgeMcpService(SqlForgeMcpProperties properties,
                                                  RestClient restClient,
                                                  AmisValidator amisValidator,
                                                  MetricsService metricsService,
                                                  AuditLogger auditLogger,
                                                  org.springframework.beans.factory.ObjectProvider<PlaywrightRenderer> playWrightRenderer) {
        return new SqlForgeMcpService(properties, restClient,
                amisValidator, metricsService, auditLogger, playWrightRenderer.getIfAvailable());
    }

    // ============ Tool 注册 ============

    /**
     * 把 5 个领域 Service + 1 个 facade 全部注册到 MCP server。
     */
    @Bean
    public ToolCallbackProvider sqlForgeMcpTools(MetadataService metadataService,
                                                JsonCrudService jsonCrudService,
                                                TemplateService templateService,
                                                AmisService amisService,
                                                SqlForgeMcpService sqlForgeMcpService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(metadataService, jsonCrudService,
                        templateService, amisService, sqlForgeMcpService)
                .build();
    }
}
