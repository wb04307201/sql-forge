package cn.wubo.sql.forge;

import cn.wubo.sql.forge.EntityExecutor;
import cn.wubo.sql.forge.cache.EntityCacheService;
import cn.wubo.sql.forge.record.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.springframework.web.servlet.function.RequestPredicates.accept;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * SQL Forge 核心自动配置类，注册数据库执行器、Record 执行器、Entity 执行器、模板引擎等基础 Bean，并按配置开关暴露 JSON CRUD、模板 SQL、数据库直连等 API 端点。
 */
@EnableCaching
@AutoConfiguration
@EnableConfigurationProperties({SqlForgeProperties.class})
public class SqlForgeConfiguration {

    @Bean
    public IExecutor databaseExecutor(DataSource dataSource, SqlForgeProperties properties) {
        return new DatabaseExecutor(dataSource,properties);
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
    public RouterFunction<ServerResponse> sqlForgeApiDatabaseRouter(SqlForgeProperties sqlForgeProperties, ExecutorService executorService, AuthFilter authFilter) {
        RouterFunctions.Builder builder = route();
        builder.POST(Constant.EXECUTE_SQL_URL, request -> {
            String executorName = request.param("executorName").orElse("database");
            SqlScript sqlScript = request.body(SqlScript.class);
            if (sqlForgeProperties.getApi().getDatabase().getSelectOnly()) {
                return ServerResponse.ok().body(executorService.getExecutor(executorName).executeQuery(sqlScript));
            } else {
                return ServerResponse.ok().body(executorService.getExecutor(executorName).execute(sqlScript));
            }
        }).GET(Constant.GET_METADATA_DATABASE_URL, request -> {
            String executorName = request.param("executorName").orElse("database");
            return ServerResponse.ok().body(executorService.getExecutor(executorName).getMetaDataDatabase());
        }).GET(Constant.GET_METADATA_TABLES_URL, request -> {
            String executorName = request.param("executorName").orElse("database");
            return ServerResponse.ok().body(executorService.getExecutor(executorName).getMetaDataTables());
        }).GET(Constant.GET_METADATA_TABLE_DEFINITIONS_URL, request -> {
            String executorName = request.param("executorName").orElse("database");
            String catalog = request.param("catalog").orElse(null);
            String schema = request.param("schema").orElse(null);
            String tableType = request.param("tableType").orElse(null);
            String tableName = request.param("tableName").orElseThrow(() -> new IllegalArgumentException("tableName is required"));
            return ServerResponse.ok().body(executorService.getExecutor(executorName).getMetaDataDefinitions(catalog, schema, tableName, tableType));
        });
        builder.filter(authFilter);

        return builder.build();
    }

    @Bean("sqlForgeApiJsonRouter")
    @ConditionalOnProperty(name = "sql.forge.api.json.enabled", havingValue = "true", matchIfMissing = true)
    public RouterFunction<ServerResponse> sqlForgeApiRouter(RecordExecutor recordExecutor, AuthFilter authFilter) {
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
        builder.filter(authFilter);
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ITemplateSqlStorage templateSqlStorage() {
        return new InMemoryTemplateSqlStorage();
    }

    @Bean
    public TemplateSqlExcutor templateExcutor(ITemplateSqlStorage templateSqlStorage, ExecutorService executorService) {
        return new TemplateSqlExcutor(templateSqlStorage, executorService);
    }

    @Bean("sqlForgeApiTemplateSqlRouter")
    @ConditionalOnProperty(name = "sql.forge.api.template.sql.enabled", havingValue = "true", matchIfMissing = true)
    public RouterFunction<ServerResponse> sqlForgeApiTemplateSqlRouter(ITemplateSqlStorage templateSqlStorage, TemplateSqlExcutor templateSqlExcutor, AuthFilter authFilter) {
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
            String description = request.param("description").orElse(null);
            String executorName = request.param("executorName").orElse(null);
            String context = request.param("context").orElse(null);
            TemplateSql template = new TemplateSql();
            template.setId(id);
            template.setDescription(description);
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
        builder.filter(authFilter);
        return builder.build();
    }

}
