package cn.wubo.sql.forge;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.Map;

/**
 * 认证过滤器，支持 Session 登录和 ApiKey 两种方式，任一通过即可访问受保护的 API。
 * 白名单路径（登录接口、静态资源）无需认证直接放行。
 */
@RequiredArgsConstructor
public class AuthFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private static final String API_KEY_HEADER = "X-Api-Key";

    private final SessionManager sessionManager;
    private final List<String> apiKeys;

    /**
     * 构造认证过滤器，仅使用 Session 校验（无 ApiKey 配置）。
     *
     * @param sessionManager 会话管理器
     */
    public AuthFilter(SessionManager sessionManager) {
        this(sessionManager, List.of());
    }

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        String path = request.path();

        if (path.startsWith("/sql/forge/api/auth") ||
            path.startsWith("/sql/forge/web/") ||
            path.equals("/sql/forge/web") ||
            path.endsWith(".html") ||
            path.endsWith(".js") ||
            path.endsWith(".css")) {
            return next.handle(request);
        }

        if (isValidApiKey(request)) {
            return next.handle(request);
        }

        if (sessionManager.isLoggedIn(request.servletRequest())) {
            return next.handle(request);
        }

        return ServerResponse.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("success", false, "msg", "未登录", "code", 401));
    }

    /**
     * 校验请求头中的 ApiKey 是否有效。
     *
     * @param request 服务端请求
     * @return ApiKey 有效返回 true，否则返回 false
     */
    private boolean isValidApiKey(ServerRequest request) {
        if (apiKeys == null || apiKeys.isEmpty()) {
            return false;
        }
        String apiKey = request.headers().firstHeader(API_KEY_HEADER);
        return apiKey != null && apiKeys.contains(apiKey);
    }
}
