package cn.wubo.sql.forge;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

import static org.springframework.web.servlet.function.RequestPredicates.accept;
import static org.springframework.web.servlet.function.RouterFunctions.route;
import org.springframework.http.MediaType;

@AutoConfiguration
@EnableConfigurationProperties(SqlForgeProperties.class)
public class WebAutoConfiguration {

    // ========== 基础 Bean ==========

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
    @ConditionalOnMissingBean(IRoleTemplateStorage.class)
    public IRoleTemplateStorage roleTemplateStorage() {
        return new InMemoryRoleTemplateStorage();
    }

    @Bean
    @ConditionalOnMissingBean(IRoleStorage.class)
    public IRoleStorage roleStorage() {
        return new InMemoryRoleStorage();
    }

    @Bean
    public SessionManager sessionManager(IUserStorage userStorage, IUserRoleStorage userRoleStorage, IRoleStorage roleStorage) {
        return new SessionManager(userStorage, userRoleStorage, roleStorage);
    }

    @Bean
    public AuthFilter authFilter(SessionManager sessionManager) {
        return new AuthFilter(sessionManager);
    }

    // ========== AMIS 模板相关 ==========

    @Bean
    @ConditionalOnMissingBean
    public ITemplateAmisStorage templateAmisStorage() {
        return new InMemoryTemplateAmisStorage();
    }

    @Bean("sqlForgeApiTemplateAmisRouter")
    @ConditionalOnProperty(name = "sql.forge.api.template.amis.enabled", havingValue = "true", matchIfMissing = true)
    public RouterFunction<ServerResponse> sqlForgeApiTemplateAmisRouter(ITemplateAmisStorage templateAmisStorage, AuthFilter authFilter) {
        return route()
            .PUT("sql/forge/api/template/amis", accept(MediaType.APPLICATION_JSON), request -> {
                TemplateAmis template = request.body(TemplateAmis.class);
                templateAmisStorage.save(template);
                return ServerResponse.ok().body(true);
            })
            .DELETE("sql/forge/api/template/amis/{id}", accept(MediaType.APPLICATION_JSON), request -> {
                templateAmisStorage.remove(request.pathVariable("id"));
                return ServerResponse.ok().body(true);
            })
            .GET("sql/forge/api/template/amis/{id}", accept(MediaType.APPLICATION_JSON), request ->
                ServerResponse.ok().body(templateAmisStorage.get(request.pathVariable("id"))))
            .GET("sql/forge/api/template/amis", accept(MediaType.APPLICATION_JSON), request -> {
                String id = request.param("id").orElse(null);
                String name = request.param("name").orElse(null);
                String description = request.param("description").orElse(null);
                String context = request.param("context").orElse(null);
                TemplateAmis filter = new TemplateAmis();
                filter.setId(id);
                filter.setName(name);
                filter.setDescription(description);
                filter.setContext(context);
                return ServerResponse.ok().body(templateAmisStorage.list(filter));
            })
            .filter(authFilter)
            .build();
    }

    // ========== 认证相关 ==========

    @Bean("sqlForgeApiAuthRouter")
    @ConditionalOnProperty(name = "sql.forge.console.enabled", havingValue = "true", matchIfMissing = true)
    public RouterFunction<ServerResponse> sqlForgeApiAuthRouter(SessionManager sessionManager, IUserRoleStorage userRoleStorage) {
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

    // ========== 用户管理相关 ==========

    @Bean("sqlForgeApiUserRouter")
    @ConditionalOnProperty(name = "sql.forge.console.enabled", havingValue = "true", matchIfMissing = true)
    public RouterFunction<ServerResponse> sqlForgeApiUserRouter(
            IUserStorage userStorage,
            IUserRoleStorage userRoleStorage,
            SessionManager sessionManager,
            AuthFilter authFilter) {
        return route()
            .GET("sql/forge/api/user", accept(MediaType.APPLICATION_JSON), request -> {
                if (!sessionManager.isAdmin(request.servletRequest())) {
                    return ServerResponse.ok().body(Map.of("status", 1, "msg", "需要管理员权限"));
                }
                User filter = new User();
                filter.setUsername(request.param("username").orElse(null));
                String category = request.param("category").orElse(null);
                String roleId = request.param("roleId").orElse(null);
                List<User> users = userStorage.list(filter);
                // 按分类过滤
                if (category != null && !category.isEmpty()) {
                    users = users.stream().filter(u -> category.equals(u.getCategory())).toList();
                }
                // 按角色过滤
                if (roleId != null && !roleId.isEmpty()) {
                    users = users.stream().filter(u -> userRoleStorage.listRoleIdsByUser(u.getId()).contains(roleId)).toList();
                }
                // 附加角色信息
                List<Map<String, Object>> result = new ArrayList<>();
                for (User u : users) {
                    Map<String, Object> userMap = new LinkedHashMap<>();
                    userMap.put("id", u.getId());
                    userMap.put("username", u.getUsername());
                    userMap.put("enabled", u.getEnabled());
                    userMap.put("category", u.getCategory());
                    userMap.put("createdTime", u.getCreatedTime());
                    userMap.put("updatedTime", u.getUpdatedTime());
                    userMap.put("roles", userRoleStorage.listRoleIdsByUser(u.getId()));
                    result.add(userMap);
                }
                return ServerResponse.ok().body(Map.of("status", 0, "data", Map.of("rows", result, "total", result.size())));
            })
            .PUT("sql/forge/api/user", accept(MediaType.APPLICATION_JSON), request -> {
                if (!sessionManager.isAdmin(request.servletRequest())) {
                    return ServerResponse.ok().body(Map.of("status", 1, "msg", "需要管理员权限"));
                }
                User user = request.body(User.class);
                if (user.getEnabled() == null) user.setEnabled(true);
                userStorage.save(user);
                return ServerResponse.ok().body(Map.of("status", 0, "msg", "保存成功", "data", Map.of("id", user.getId())));
            })
            .DELETE("sql/forge/api/user/{id}", accept(MediaType.APPLICATION_JSON), request -> {
                if (!sessionManager.isAdmin(request.servletRequest())) {
                    return ServerResponse.ok().body(Map.of("status", 1, "msg", "需要管理员权限"));
                }
                userStorage.remove(request.pathVariable("id"));
                return ServerResponse.ok().body(Map.of("status", 0, "msg", "删除成功"));
            })
            .filter(authFilter)
            .build();
    }

    // ========== 角色管理相关 ==========

    @Bean("sqlForgeApiRoleRouter")
    @ConditionalOnProperty(name = "sql.forge.console.enabled", havingValue = "true", matchIfMissing = true)
    public RouterFunction<ServerResponse> sqlForgeApiRoleRouter(
            IRoleStorage roleStorage,
            IRoleTemplateStorage roleTemplateStorage,
            SessionManager sessionManager,
            AuthFilter authFilter) {
        return route()
            .GET("sql/forge/api/role", accept(MediaType.APPLICATION_JSON), request -> {
                Role filter = new Role();
                filter.setName(request.param("name").orElse(null));
                List<Role> roles = roleStorage.list(filter);
                return ServerResponse.ok().body(Map.of("status", 0, "data", Map.of("items", roles, "total", roles.size())));
            })
            .PUT("sql/forge/api/role", accept(MediaType.APPLICATION_JSON), request -> {
                if (!sessionManager.isAdmin(request.servletRequest())) {
                    return ServerResponse.ok().body(Map.of("status", 1, "msg", "需要管理员权限"));
                }
                Role role = request.body(Role.class);
                roleStorage.save(role);
                return ServerResponse.ok().body(Map.of("status", 0, "msg", "保存成功"));
            })
            .DELETE("sql/forge/api/role/{id}", accept(MediaType.APPLICATION_JSON), request -> {
                if (!sessionManager.isAdmin(request.servletRequest())) {
                    return ServerResponse.ok().body(Map.of("status", 1, "msg", "需要管理员权限"));
                }
                try {
                    roleStorage.remove(request.pathVariable("id"));
                    return ServerResponse.ok().body(Map.of("status", 0, "msg", "删除成功"));
                } catch (UnsupportedOperationException e) {
                    return ServerResponse.ok().body(Map.of("status", 1, "msg", e.getMessage()));
                }
            })
            .GET("sql/forge/api/role-template", accept(MediaType.APPLICATION_JSON), request -> {
                String role = request.param("role").orElse(null);
                if (role == null || role.isEmpty()) {
                    return ServerResponse.ok().body(Map.of("templateIds", new ArrayList<String>()));
                }
                return ServerResponse.ok().body(Map.of("templateIds", roleTemplateStorage.listTemplateIdsByRole(role)));
            })
            .PUT("sql/forge/api/role-template", accept(MediaType.APPLICATION_JSON), request -> {
                if (!sessionManager.isAdmin(request.servletRequest())) {
                    return ServerResponse.ok().body(Map.of("success", false, "msg", "需要管理员权限"));
                }
                Map<String, Object> body = request.body(Map.class);
                String role = (String) body.get("role");
                @SuppressWarnings("unchecked")
                List<String> templateIds = (List<String>) body.get("templateIds");
                roleTemplateStorage.removeAllByRole(role);
                if (templateIds != null) {
                    for (String templateId : templateIds) {
                        RoleTemplate rt = new RoleTemplate();
                        rt.setRoleId(role);
                        rt.setTemplateId(templateId);
                        roleTemplateStorage.save(rt);
                    }
                }
                return ServerResponse.ok().body(true);
            })
            .filter(authFilter)
            .build();
    }

    // ========== 用户-角色关联 API ==========

    @Bean("sqlForgeApiUserRoleRouter")
    @ConditionalOnProperty(name = "sql.forge.console.enabled", havingValue = "true", matchIfMissing = true)
    public RouterFunction<ServerResponse> sqlForgeApiUserRoleRouter(
            IUserRoleStorage userRoleStorage,
            SessionManager sessionManager,
            AuthFilter authFilter) {
        return route()
            .GET("sql/forge/api/user-role", accept(MediaType.APPLICATION_JSON), request -> {
                if (!sessionManager.isAdmin(request.servletRequest())) {
                    return ServerResponse.ok().body(Map.of("success", false, "msg", "需要管理员权限"));
                }
                String userId = request.param("userId").orElse(null);
                if (userId == null) {
                    return ServerResponse.ok().body(Map.of("success", false, "msg", "缺少 userId 参数"));
                }
                return ServerResponse.ok().body(Map.of("status", 0, "data", Map.of("roleIds", userRoleStorage.listRoleIdsByUser(userId))));
            })
            .PUT("sql/forge/api/user-role", accept(MediaType.APPLICATION_JSON), request -> {
                if (!sessionManager.isAdmin(request.servletRequest())) {
                    return ServerResponse.ok().body(Map.of("success", false, "msg", "需要管理员权限"));
                }
                Map<String, Object> body = request.body(Map.class);
                String userId = (String) body.get("userId");
                @SuppressWarnings("unchecked")
                List<String> roleIds = (List<String>) body.get("roleIds");
                userRoleStorage.removeAllByUser(userId);
                if (roleIds != null) {
                    for (String roleId : roleIds) {
                        UserRole ur = new UserRole();
                        ur.setUserId(userId);
                        ur.setRoleId(roleId);
                        userRoleStorage.save(ur);
                    }
                }
                return ServerResponse.ok().body(true);
            })
            .filter(authFilter)
            .build();
    }

    // ========== 控制台 & Home 路由 ==========

    @Bean("sqlForgeApiDatabaseConsoleRouter")
    @ConditionalOnProperty(name = "sql.forge.api.database.enabled", havingValue = "true")
    @ConditionalOnProperty(name = "sql.forge.console.enabled", havingValue = "true", matchIfMissing = true)
    public RouterFunction<ServerResponse> sqlForgeApiDatabaseConsoleRouter(ExecutorService executorService, AuthFilter authFilter) {
        return route()
            .GET("sql/forge/api/database/metaDataTree", request -> {
                String executorName = request.param("executorName").orElse("database");
                return ServerResponse.ok().body(executorService.getExecutor(executorName).getMetaDataTree());
            })
            .GET("sql/forge/api/database/getMetaDataDatabase", request -> {
                String executorName = request.param("executorName").orElse("database");
                return ServerResponse.ok().body(executorService.getExecutor(executorName).getMetaDataDatabase());
            })
            .GET("sql/forge/api/database/metaDataTables", request -> {
                String executorName = request.param("executorName").orElse("database");
                return ServerResponse.ok().body(executorService.getExecutor(executorName).getMetaDataTables());
            })
            .filter(authFilter)
            .build();
    }

    @Bean("sqlForgeConsoleRouter")
    @ConditionalOnProperty(name = "sql.forge.console.enabled", havingValue = "true", matchIfMissing = true)
    public RouterFunction<ServerResponse> sqlForgeConsoleRouter(
            SqlForgeProperties sqlForgeProperties,
            ExecutorService executorService,
            SessionManager sessionManager) {
        return route()
            .GET("sql/forge/web", request -> {
                User user = sessionManager.getCurrentUser(request.servletRequest());
                String target = user != null ? "/sql/forge/web/home.html" : "/sql/forge/web/login.html";
                String redirectUrl = UriComponentsBuilder.fromPath(target)
                        .queryParams(request.params())
                        .build()
                        .toUriString();
                return ServerResponse.temporaryRedirect(URI.create(redirectUrl)).build();
            })
            .GET("sql/forge/web/login", request ->
                ServerResponse.temporaryRedirect(
                    URI.create(UriComponentsBuilder.fromPath("/sql/forge/web/login.html")
                        .queryParams(request.params()).build().toUriString())
                ).build())
            .GET("sql/forge/web/home", request ->
                ServerResponse.temporaryRedirect(
                    URI.create(UriComponentsBuilder.fromPath("/sql/forge/web/home.html")
                        .queryParams(request.params()).build().toUriString())
                ).build())
            .GET("sql/forge/api/console/executorName", request -> ServerResponse.ok().body(executorService.getExecutorNames()))
            .GET("sql/forge/web/api/state", request -> ServerResponse.ok().body(sqlForgeProperties.getApi()))
            .build();
    }
}
