package cn.wubo.sql.forge;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 执行器服务，管理所有已注册的 {@link IExecutor} 实例，按名称路由到对应的数据源执行器。
 *
 * @param executors 已注册的执行器列表
 */
public record ExecutorService(List<IExecutor> executors) {

    /**
     * 获取默认执行器（名称为 "database"）。
     *
     * @return 默认数据库执行器
     */
    public IExecutor getExecutor() {
        return getExecutor("database");
    }

    /**
     * 按名称获取指定的执行器。
     *
     * @param executorName 执行器名称
     * @return 对应的执行器实例
     * @throws IllegalArgumentException 未找到对应名称的执行器
     */
    public IExecutor getExecutor(@NotBlank String executorName) {
        for (IExecutor executor : executors) {
            if (executorName.equals(executor.getExecutorName())) return executor;
        }
        throw new IllegalArgumentException("未找到对应的数据源执行器");
    }

    /**
     * 获取所有已注册的执行器名称列表。
     *
     * @return 执行器名称列表
     */
    public List<String> getExecutorNames(){
        return executors.stream().map(IExecutor::getExecutorName).toList();
    }
}
