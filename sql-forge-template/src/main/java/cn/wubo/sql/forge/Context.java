package cn.wubo.sql.forge;

import cn.wubo.sql.forge.map.ParamMap;
import lombok.Getter;

import java.util.*;

public class Context {
    @Getter
    private final GenerationSqlMode mode;
    private final Map<String, Object> variables; // 原始变量
    private final ParamMap params = new ParamMap(); // 按顺序收集的参数
    private final StringBuilder sqlBuilder = new StringBuilder(); // SQL 构建器
    private final Stack<Map<String, Object>> scopeStack = new Stack<>(); // 作用域栈（支持变量覆盖）

    public Context(Map<String, Object> variables, GenerationSqlMode mode) {
        this.mode = mode;
        this.variables = variables;
        this.scopeStack.push(new HashMap<>(variables)); // 初始作用域
    }

    // 进入新作用域（如 foreach 循环）
    public void enterScope() {
        scopeStack.push(new HashMap<>(getCurrentScope()));
    }

    // 退出作用域
    public void exitScope() {
        if (scopeStack.size() > 1) { // 保留初始作用域
            scopeStack.pop();
        }
    }

    // 获取当前作用域变量
    public Map<String, Object> getCurrentScope() {
        return scopeStack.peek();
    }

    // 在当前作用域设置变量
    public void setVariable(String name, Object value) {
        getCurrentScope().put(name, value);
    }

    // 获取变量（优先当前作用域）
    public Object getVariable(String name) {
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Object val = scopeStack.get(i).get(name);
            if (val != null) return val;
        }
        return variables.get(name); // 回退到原始变量
    }

    public void addParam(Object value) {
        params.put(value);
    }

    public void appendSql(String sql) {
        sqlBuilder.append(sql);
    }

    public String getSql() {
        return sqlBuilder.toString();
    }

    public ParamMap getParamMap() {
        return params;
    }
}
