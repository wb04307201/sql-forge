package cn.wubo.sql.forge;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

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

    public User getCurrentUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(SESSION_USER);
    }

    public List<String> getCurrentUserRoles(HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (user == null) return List.of();
        return userRoleStorage.listRoleIdsByUser(user.getId());
    }

    public void logout(HttpServletRequest request) {
        request.getSession().removeAttribute(SESSION_USER);
    }

    public boolean isLoggedIn(HttpServletRequest request) {
        return getCurrentUser(request) != null;
    }

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
