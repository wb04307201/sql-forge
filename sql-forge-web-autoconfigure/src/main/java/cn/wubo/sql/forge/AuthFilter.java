package cn.wubo.sql.forge;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;

/**
 * 认证过滤器，拦截需要登录的 API 请求，对白名单路径（登录、静态资源）放行。
 */
@RequiredArgsConstructor
public class AuthFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private final SessionManager sessionManager;

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        String path = request.path().toString();

        if (path.startsWith("/sql/forge/api/auth") ||
            path.startsWith("/sql/forge/web/") ||
            path.equals("/sql/forge/web") ||
            path.endsWith(".html") ||
            path.endsWith(".js") ||
            path.endsWith(".css")) {
            return next.handle(request);
        }

        if (!sessionManager.isLoggedIn(request.servletRequest())) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "msg", "未登录", "code", 401));
        }

        return next.handle(request);
    }
}
