package cn.wubo.sql.forge;

import cn.eubo.sql.forge.EntityExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JdbcStorageConfiguration {

    @Bean
    @Primary
    public IUserStorage jdbcUserStorage(EntityExecutor entityExecutor) {
        return new JdbcUserStorage(entityExecutor);
    }

    @Bean
    @Primary
    public IRoleStorage jdbcRoleStorage(EntityExecutor entityExecutor) {
        return new JdbcRoleStorage(entityExecutor);
    }

    @Bean
    @Primary
    public IUserRoleStorage jdbcUserRoleStorage(EntityExecutor entityExecutor) {
        return new JdbcUserRoleStorage(entityExecutor);
    }

    @Bean
    @Primary
    public IRoleTemplateStorage jdbcRoleTemplateStorage(EntityExecutor entityExecutor) {
        return new JdbcRoleTemplateStorage(entityExecutor);
    }
}
