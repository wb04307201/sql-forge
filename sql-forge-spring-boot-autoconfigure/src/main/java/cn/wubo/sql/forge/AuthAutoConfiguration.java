package cn.wubo.sql.forge;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.Map;

import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * 认证鉴权自动配置类，注册用户存储、会话管理器和认证过滤器等 Bean，并提供登录/登出/状态查询 API。
 */
@AutoConfiguration
@EnableConfigurationProperties(SqlForgeProperties.class)
public class AuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IUserStorage.class)
    public IUserStorage userStorage() {
        return new InMemoryUserStorage();
    }

    @Bean
    @ConditionalOnMissingBean(IUserRoleStorage.class)
    public IUserRoleStorage userRoleStorage() {
        return new InMemoryUserRoleStorage();
    }

    @Bean
    @ConditionalOnMissingBean(SessionManager.class)
    public SessionManager sessionManager(IUserStorage userStorage, IUserRoleStorage userRoleStorage) {
        return new SessionManager(userStorage, userRoleStorage);
    }

    @Bean
    @ConditionalOnMissingBean(AuthFilter.class)
    public AuthFilter authFilter(SessionManager sessionManager, SqlForgeProperties properties) {
        return new AuthFilter(sessionManager, properties.getApiKeys());
    }

    /**
     * 认证相关 API 路由（登录、登出、状态查询），无需认证即可访问。
     *
     * @param sessionManager    会话管理器
     * @param userRoleStorage   用户-角色关联存储
     * @return 认证 API 路由函数
     */
    @Bean("sqlForgeApiAuthRouter")
    @ConditionalOnProperty(name = "sql.forge.console.enabled", havingValue = "true", matchIfMissing = true)
    public RouterFunction<ServerResponse> sqlForgeApiAuthRouter(
            SessionManager sessionManager, IUserRoleStorage userRoleStorage) {
        return route()
            .POST("sql/forge/api/auth/login", request -> {
                Map<String, String> body = request.body(Map.class);
                String username = body.get("username");
                String password = body.get("password");
                SessionManager.LoginResult result = sessionManager.login(
                    request.servletRequest(), username, password);
                if (result.success()) {
                    List<String> roles = userRoleStorage.listRoleIdsByUser(result.user().getId());
                    return ServerResponse.ok().body(Map.of(
                        "success", true,
                        "msg", "登录成功",
                        "data", Map.of("username", result.user().getUsername(), "category", result.user().getCategory(), "roles", roles)
                    ));
                } else {
                    return ServerResponse.ok().body(Map.of("success", false, "msg", result.message()));
                }
            })
            .POST("sql/forge/api/auth/logout", request -> {
                sessionManager.logout(request.servletRequest());
                return ServerResponse.ok().body(Map.of("success", true, "msg", "已退出"));
            })
            .GET("sql/forge/api/auth/status", request -> {
                User user = sessionManager.getCurrentUser(request.servletRequest());
                if (user != null) {
                    List<String> roles = userRoleStorage.listRoleIdsByUser(user.getId());
                    return ServerResponse.ok().body(Map.of(
                        "success", true,
                        "data", Map.of("loggedIn", true, "username", user.getUsername(), "category", user.getCategory(), "roles", roles)
                    ));
                } else {
                    return ServerResponse.ok().body(Map.of("success", true, "data", Map.of("loggedIn", false)));
                }
            })
            .GET("sql/forge/api/auth/user", request -> {
                User user = sessionManager.getCurrentUser(request.servletRequest());
                if (user != null) {
                    List<String> roles = userRoleStorage.listRoleIdsByUser(user.getId());
                    return ServerResponse.ok().body(Map.of(
                        "success", true,
                        "data", Map.of("username", user.getUsername(), "category", user.getCategory(), "roles", roles)
                    ));
                } else {
                    return ServerResponse.ok().body(Map.of("success", false, "msg", "未登录"));
                }
            })
            .build();
    }
}
