package cn.wubo.sql.forge;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * JDBC 存储配置类，注册基于 ExecutorService 的用户、角色、模板存储 Bean。
 */
@Configuration
public class JdbcStorageConfiguration {

    @Bean
    @Primary
    public IUserStorage jdbcUserStorage(ExecutorService executorService) {
        return new JdbcUserStorage(executorService);
    }

    @Bean
    @Primary
    public IRoleStorage jdbcRoleStorage(ExecutorService executorService) {
        return new JdbcRoleStorage(executorService);
    }

    @Bean
    @Primary
    public IUserRoleStorage jdbcUserRoleStorage(ExecutorService executorService) {
        return new JdbcUserRoleStorage(executorService);
    }

    @Bean
    @Primary
    public IRoleTemplateStorage jdbcRoleTemplateStorage(ExecutorService executorService) {
        return new JdbcRoleTemplateStorage(executorService);
    }
}
