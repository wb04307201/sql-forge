package cn.wubo.sql.forge;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ExecutorService(List<IExecutor> executors) {

    public IExecutor getExecutor() {
        return getExecutor("database");
    }

    public IExecutor getExecutor(@NotBlank String executorName) {
        for (IExecutor executor : executors) {
            if (executorName.equals(executor.getExecutorName())) return executor;
        }
        throw new IllegalArgumentException("未找到对应的数据源执行器");
    }

    public List<String> getExecutorNames(){
        return executors.stream().map(IExecutor::getExecutorName).toList();
    }
}
