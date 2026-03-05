package cn.wubo.sql.forge;

import cn.eubo.sql.forge.EntityExecutor;
import cn.eubo.sql.forge.cache.EntityCacheService;
import cn.wubo.sql.forge.record.*;
import cn.wubo.sql.forge.records.SqlScript;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import javax.sql.DataSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.springframework.web.servlet.function.RequestPredicates.accept;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@EnableCaching
@AutoConfiguration
@EnableConfigurationProperties({SqlForgeProperties.class})
@AutoConfigureAfter(name = {
        // ChatModel
        "org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration",
        "org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiChatAutoConfiguration",
        "org.springframework.ai.model.bedrock.autoconfigure.BedrockAiChatAutoConfiguration",
        "org.springframework.ai.model.deepseek.autoconfigure.DeepSeekChatAutoConfiguration",
        "org.springframework.ai.model.elevenlabs.autoconfigure.ElevenLabsChatAutoConfiguration",
        "org.springframework.ai.model.google.genai.autoconfigure.GoogleGenAiChatAutoConfiguration",
        "org.springframework.ai.model.huggingface.autoconfigure.HuggingFaceChatAutoConfiguration",
        "org.springframework.ai.model.minimax.autoconfigure.MinimaxChatAutoConfiguration",
        "org.springframework.ai.model.mistralai.autoconfigure.MistralAiChatAutoConfiguration",
        "org.springframework.ai.model.oci.genai.autoconfigure.OciGenAiChatAutoConfiguration",
        "org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
        "org.springframework.ai.model.openaisdk.autoconfigure.OpenAiSdkEmbeddingAutoConfiguration",
        "org.springframework.ai.model.postgresml.autoconfigure.PostgresMlEmbeddingAutoConfiguration",
        "org.springframework.ai.model.stabilityai.autoconfigure.StabilityAiChatAutoConfiguration",
        "org.springframework.ai.model.transformers.autoconfigure.TransformersChatAutoConfiguration",
        "org.springframework.ai.model.vertexai.autoconfigure.VertexAiChatAutoConfiguration",
        "org.springframework.ai.model.zhipuai.autoconfigure.ZhipuAiChatAutoConfiguration",
        // Vectorstore
        "org.springframework.ai.vectorstore.azure.autoconfigure.AzureVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.cosmosdb.autoconfigure.CosmosDBVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.cassandra.autoconfigure.CassandraVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.couchbase.autoconfigure.CouchbaseSearchVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.elasticsearch.autoconfigure.ElasticsearchVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.gemfire.autoconfigure.GemFireVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.mariadb.autoconfigure.MariaDbStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.mongodb.autoconfigure.MongoDBAtlasVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.neo4j.autoconfigure.Neo4jVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.observation.autoconfigure.VectorStoreObservationAutoConfiguration",
        "org.springframework.ai.vectorstore.opensearch.autoconfigure.OpenSearchVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.oracle.autoconfigure.OracleVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.pinecone.autoconfigure.PineconeVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.redis.autoconfigure.RedisVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.typesense.autoconfigure.TypesenseVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.weaviate.autoconfigure.WeaviateVectorStoreAutoConfiguration"
})
public class SqlForgeConfiguration implements WebMvcConfigurer {
    // ... existing code ...
    @Bean
    public IExecutor databaseExecutor(DataSource dataSource) {
        return new DatabaseExecutor(dataSource);
    }

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
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> modelMap = mapper.readValue(inputStream, new TypeReference<>() {
            });
            String model = mapper.writeValueAsString(modelMap);
            return new CalciteExcutor(model);
        }
    }

    @Bean
    public ExecutorService executorService(List<IExecutor> executors) {
        return new ExecutorService(executors);
    }

    @Bean
    public RecordExecutor recordExecutor(
            ExecutorService executorService,
            @Autowired(required = false) List<IBeforeRecordExecutor<Delete>> deleteExecutes,
            @Autowired(required = false) List<IBeforeRecordExecutor<Insert>> insertExecutes,
            @Autowired(required = false) List<IBeforeRecordExecutor<Select>> selectExecutes,
            @Autowired(required = false) List<IBeforeRecordExecutor<SelectPage>> selectPageExecutes,
            @Autowired(required = false) List<IBeforeRecordExecutor<Update>> updateExecutes
    ) {
        return new RecordExecutor(executorService, deleteExecutes, insertExecutes, selectExecutes, selectPageExecutes, updateExecutes);
    }

    @Bean
    public EntityCacheService entityCacheService() {
        return new EntityCacheService();
    }

    @Bean
    public EntityExecutor entityExecutor(RecordExecutor recordExecutor, EntityCacheService entityCacheService) {
        return new EntityExecutor(recordExecutor, entityCacheService);
    }

    @Bean("sqlForgeApiDatabaseRouter")
    @ConditionalOnProperty(name = "sql.forge.api.database.enabled", havingValue = "true")
    public RouterFunction<ServerResponse> sqlForgeApiDatabaseRouter(SqlForgeProperties sqlForgeProperties, ExecutorService executorService) {
        RouterFunctions.Builder builder = route();
        builder.POST("/sql/forge/api/database/execute", request -> {
            String executorName = request.param("executorName").orElse("database");
            SqlScript sqlScript = request.body(SqlScript.class);
            if (sqlForgeProperties.getApi().getDatabase().getSelectOnly()) {
                return ServerResponse.ok().body(executorService.getExecutor(executorName).executeQuery(sqlScript));
            } else {
                return ServerResponse.ok().body(executorService.getExecutor(executorName).execute(sqlScript));
            }
        });

        return builder.build();
    }

    @Bean("sqlForgeApiJsonRouter")
    @ConditionalOnProperty(name = "sql.forge.api.json.enabled", havingValue = "true", matchIfMissing = true)
    public RouterFunction<ServerResponse> sqlForgeApiRouter(RecordExecutor recordExecutor) {
        RouterFunctions.Builder builder = route();
        builder.POST("sql/forge/api/json/{method}/{tableName}", accept(MediaType.APPLICATION_JSON), request -> {
            String executorName = request.param("executorName").orElse("database");
            String method = request.pathVariable("method");
            String tableName = request.pathVariable("tableName");
            Object obj = switch (method) {
                case "delete" -> recordExecutor.delete(executorName, tableName, request.body(Delete.class));
                case "insert" -> recordExecutor.insert(executorName, tableName, request.body(Insert.class));
                case "select" -> recordExecutor.select(executorName, tableName, request.body(Select.class));
                case "selectPage" -> recordExecutor.selectPage(executorName, tableName, request.body(SelectPage.class));
                case "update" -> recordExecutor.update(executorName, tableName, request.body(Update.class));
                default -> throw new IllegalArgumentException("method not found");
            };
            return ServerResponse.ok().body(obj);
        });
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ITemplateSqlStorage templateSqlStorage() {
        return new TemplateSqlStorage();
    }

    @Bean
    public TemplateSqlExcutor templateExcutor(ITemplateSqlStorage templateSqlStorage, ExecutorService executorService) {
        return new TemplateSqlExcutor(templateSqlStorage, executorService);
    }

    @Bean("sqlForgeApiTemplateSqlRouter")
    @ConditionalOnProperty(name = "sql.forge.api.template.sql.enabled", havingValue = "true", matchIfMissing = true)
    public RouterFunction<ServerResponse> sqlForgeApiTemplateSqlRouter(ITemplateSqlStorage templateSqlStorage, TemplateSqlExcutor templateSqlExcutor) {
        RouterFunctions.Builder builder = route();
        builder.PUT("sql/forge/api/template/sql", accept(MediaType.APPLICATION_JSON), request -> {
            TemplateSql template = request.body(TemplateSql.class);
            templateSqlStorage.save(template);
            return ServerResponse.ok().body(true);
        });
        builder.DELETE("sql/forge/api/template/sql/{id}", accept(MediaType.APPLICATION_JSON), request -> {
            String id = request.pathVariable("id");
            templateSqlStorage.remove(id);
            return ServerResponse.ok().body(true);
        });
        builder.GET("sql/forge/api/template/sql/{id}", accept(MediaType.APPLICATION_JSON), request -> {
            String id = request.pathVariable("id");
            return ServerResponse.ok().body(templateSqlStorage.get(id));
        });
        builder.GET("sql/forge/api/template/sql", accept(MediaType.APPLICATION_JSON), request -> {
            String id = request.param("id").orElse(null);
            String executorName = request.param("executorName").orElse(null);
            String context = request.param("context").orElse(null);
            TemplateSql template = new TemplateSql();
            template.setId(id);
            template.setExecutorName(executorName);
            template.setContext(context);
            return ServerResponse.ok().body(templateSqlStorage.list(template));
        });
        builder.POST("sql/forge/api/template/sql/{id}", accept(MediaType.APPLICATION_JSON), request -> {
            String id = request.pathVariable("id");
            Map<String, Object> params = request.body(new ParameterizedTypeReference<>() {
            });
            return ServerResponse.ok().body(templateSqlExcutor.execute(id, params));
        });
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ITemplateAmisStorage templateAmisStorage() {
        return new TemplateAmisStorage();
    }

    @Bean("sqlForgeApiTemplateAmisRouter")
    @ConditionalOnProperty(name = "sql.forge.api.template.amis.enabled", havingValue = "true", matchIfMissing = true)
    public RouterFunction<ServerResponse> sqlForgeApiTemplateAmisRouter(ITemplateAmisStorage templateAmisStorage) {
        RouterFunctions.Builder builder = route();
        builder.PUT("sql/forge/api/template/amis", accept(MediaType.APPLICATION_JSON), request -> {
            TemplateAmis template = request.body(TemplateAmis.class);
            templateAmisStorage.save(template);
            return ServerResponse.ok().body(true);
        });
        builder.DELETE("sql/forge/api/template/amis/{id}", accept(MediaType.APPLICATION_JSON), request -> {
            String id = request.pathVariable("id");
            templateAmisStorage.remove(id);
            return ServerResponse.ok().body(true);
        });
        builder.GET("sql/forge/api/template/amis/{id}", accept(MediaType.APPLICATION_JSON), request -> {
            String id = request.pathVariable("id");
            return ServerResponse.ok().body(templateAmisStorage.get(id));
        });
        builder.GET("sql/forge/api/template/amis", accept(MediaType.APPLICATION_JSON), request -> {
            String id = request.param("id").orElse(null);
            String context = request.param("context").orElse(null);
            TemplateAmis template = new TemplateAmis();
            template.setId(id);
            template.setContext(context);
            return ServerResponse.ok().body(templateAmisStorage.list(template));
        });
        return builder.build();
    }

    @Bean("sqlForgeApiDatabaseConsoleRouter")
    @ConditionalOnProperty(name = "sql.forge.api.database.enabled", havingValue = "true")
    @ConditionalOnProperty(name = "sql.forge.console.enabled", havingValue = "true", matchIfMissing = true)
    public RouterFunction<ServerResponse> sqlForgeApiDatabaseConsoleRouter(ExecutorService executorService) {
        RouterFunctions.Builder builder = route();
        builder.GET("sql/forge/api/database/metaData", request -> {
            String executorName = request.param("executorName").orElse("database");
            return ServerResponse.ok().body(executorService.getExecutor(executorName).getMetaData());
        });
        return builder.build();
    }

    @Bean("sqlForgeConsoleRouter")
    @ConditionalOnProperty(name = "sql.forge.console.enabled", havingValue = "true", matchIfMissing = true)
    public RouterFunction<ServerResponse> sqlForgeConsoleRouter(SqlForgeProperties sqlForgeProperties, ExecutorService executorService) {
        RouterFunctions.Builder builder = RouterFunctions.route();
        builder.GET("sql/forge/console", request -> {
            String redirectUrl = UriComponentsBuilder.fromPath("/sql/forge/console/index.html")
                    .queryParams(request.params())
                    .build()
                    .toUriString();
            return ServerResponse.temporaryRedirect(URI.create(redirectUrl)).build();
        });
        builder.GET("sql/forge/api/console/executorName", request -> ServerResponse.ok().body(executorService.getExecutorNames()));
        builder.GET("sql/forge/console/api/state", request -> ServerResponse.ok().body(sqlForgeProperties.getApi()));
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(name = "sql.forge.ai.enabled", havingValue = "true")
    public ChatClient chatClient(ChatModel chatModel) {
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(name = "sql.forge.ai.enabled", havingValue = "true")
    @ConditionalOnProperty(name = "sql.forge.console.enabled", havingValue = "true", matchIfMissing = true)
    public RouterFunction<ServerResponse> sqlForgeAiRouter(SqlForgeProperties sqlForgeProperties, ChatClient chatClient) {
        RouterFunctions.Builder builder = RouterFunctions.route();
        builder.POST("sql/forge/ai", request -> {
            AiRequest aiRequest = request.body(AiRequest.class);
            String template = sqlForgeProperties.getAi().getPromptTemplate();
            template = template.replace("{{API_SPEC}}", sqlForgeProperties.getAi().getApiSpec());
            template = template.replace("{{TABLE_INFO}}", aiRequest.tableInfo());
            template = template.replace("{{EXAMPLE_TABLE_INFO}}", sqlForgeProperties.getAi().getExampleTableInfo());
            template = template.replace("{{EXAMPLE_AMIS_INFO}}", sqlForgeProperties.getAi().getExampleAmisInfo());
            Flux<String> aiResponse = chatClient.prompt().user(template).stream().content();
            return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM)
                    .header("Cache-Control", "no-cache")
                    .header("Connection", "keep-alive")
                    .body(aiResponse);
        });

        return builder.build();
    }
}
