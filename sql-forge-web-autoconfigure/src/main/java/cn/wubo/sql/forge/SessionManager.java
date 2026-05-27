package cn.wubo.sql.forge;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 会话管理器，基于 HTTP Session 实现用户登录、登出和权限校验。
 */
public class SessionManager {

    private static final String SESSION_USER = "sql-forge-user";
    private final IUserStorage userStorage;
    private final IUserRoleStorage userRoleStorage;
    private final IRoleStorage roleStorage;

    public SessionManager(IUserStorage userStorage, IUserRoleStorage userRoleStorage, IRoleStorage roleStorage) {
        this.userStorage = userStorage;
        this.userRoleStorage = userRoleStorage;
        this.roleStorage = roleStorage;
    }

    /**
     * 用户登录，校验用户名和密码，成功后将用户信息存入 Session。
     *
     * @param request  HTTP 请求
     * @param username 用户名
     * @param password 密码
     * @return 登录结果，包含成功/失败状态、用户信息和失败原因
     */
    public LoginResult login(HttpServletRequest request, String username, String password) {
        User user = userStorage.findByUsername(username);
        if (user == null) {
            return LoginResult.fail("用户不存在");
        }
        if (!user.getPassword().equals(password)) {
            return LoginResult.fail("密码错误");
        }
        if (user.getEnabled() == null || !user.getEnabled()) {
            return LoginResult.fail("用户已禁用");
        }
        request.getSession().setAttribute(SESSION_USER, user);
        return LoginResult.success(user);
    }

    /**
     * 获取当前登录用户信息。
     *
     * @param request HTTP 请求
     * @return 当前登录用户，未登录时返回 null
     */
    public User getCurrentUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(SESSION_USER);
    }

    /**
     * 获取当前登录用户的角色列表。
     *
     * @param request HTTP 请求
     * @return 角色 ID 列表，未登录时返回空列表
     */
    public List<String> getCurrentUserRoles(HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (user == null) return List.of();
        return userRoleStorage.listRoleIdsByUser(user.getId());
    }

    /**
     * 用户登出，清除 Session 中的用户信息。
     *
     * @param request HTTP 请求
     */
    public void logout(HttpServletRequest request) {
        request.getSession().removeAttribute(SESSION_USER);
    }

    /**
     * 判断当前用户是否已登录。
     *
     * @param request HTTP 请求
     * @return 已登录返回 true，否则返回 false
     */
    public boolean isLoggedIn(HttpServletRequest request) {
        return getCurrentUser(request) != null;
    }

    /**
     * 判断当前登录用户是否为管理员。
     *
     * @param request HTTP 请求
     * @return 用户已登录且分类为 "admin" 时返回 true，否则返回 false
     */
    public boolean isAdmin(HttpServletRequest request) {
        User user = getCurrentUser(request);
        return user != null && "admin".equals(user.getCategory());
    }

    public record LoginResult(boolean success, User user, String message) {
        public static LoginResult success(User user) {
            return new LoginResult(true, user, null);
        }
        public static LoginResult fail(String message) {
            return new LoginResult(false, null, message);
        }
    }
}
