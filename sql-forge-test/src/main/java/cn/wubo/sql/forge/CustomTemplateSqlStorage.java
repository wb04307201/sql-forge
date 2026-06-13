package cn.wubo.sql.forge;

import cn.wubo.sql.forge.Entity;
import cn.wubo.sql.forge.EntityExecutor;
import cn.wubo.sql.forge.entity.EntitySelect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class CustomTemplateSqlStorage implements ITemplateSqlStorage<TemplateSql> {

    private final EntityExecutor entityExecutor;

    public CustomTemplateSqlStorage(EntityExecutor entityExecutor) {
        this.entityExecutor = entityExecutor;
    }

    @Override
    public void save(TemplateSql template) {
        try {
            List list = entityExecutor.run("database", Entity.select(SqlForgeTemplateSql.class).eq(SqlForgeTemplateSql::getId, template.getId()));
            if (list.isEmpty()) {
                entityExecutor.run("database", Entity.insert(SqlForgeTemplateSql.class)
                        .set(SqlForgeTemplateSql::getName, template.getName())
                        .set(SqlForgeTemplateSql::getDescription, template.getDescription())
                        .set(SqlForgeTemplateSql::getExecutorName, template.getExecutorName())
                        .set(SqlForgeTemplateSql::getContext, template.getContext())
                        .set(SqlForgeTemplateSql::getId, template.getId()));
            } else {
                entityExecutor.run("database", Entity.update(SqlForgeTemplateSql.class)
                        .set(SqlForgeTemplateSql::getName, template.getName())
                        .set(SqlForgeTemplateSql::getDescription, template.getDescription())
                        .set(SqlForgeTemplateSql::getExecutorName, template.getExecutorName())
                        .set(SqlForgeTemplateSql::getContext, template.getContext())
                        .eq(SqlForgeTemplateSql::getId, template.getId()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public TemplateSql get(String id) {
        try {
            List<SqlForgeTemplateSql> list = entityExecutor.run("database", Entity.select(SqlForgeTemplateSql.class).eq(SqlForgeTemplateSql::getId, id));
            if (list.isEmpty()) {
                throw new RuntimeException("模板未找到");
            } else {
                SqlForgeTemplateSql sqlForgeTemplate = list.get(0);
                TemplateSql template = new TemplateSql();
                template.setId(sqlForgeTemplate.getId());
                template.setName(sqlForgeTemplate.getName());
                template.setDescription(sqlForgeTemplate.getDescription());
                template.setExecutorName(sqlForgeTemplate.getExecutorName());
                template.setContext(sqlForgeTemplate.getContext());
                return template;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void remove(String id) {
        try {
            entityExecutor.run("database", Entity.delete(SqlForgeTemplateSql.class).eq(SqlForgeTemplateSql::getId, id));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<TemplateSql> list(TemplateSql template) {
        try {
            EntitySelect<SqlForgeTemplateSql> select = Entity.select(SqlForgeTemplateSql.class);
            if (StringUtils.hasText(template.getId()))
                select.like(SqlForgeTemplateSql::getId, template.getId());
            if (StringUtils.hasText(template.getName()))
                select.like(SqlForgeTemplateSql::getName, template.getName());
            if (StringUtils.hasText(template.getDescription()))
                select.like(SqlForgeTemplateSql::getDescription, template.getDescription());
            if (StringUtils.hasText(template.getExecutorName()))
                select.eq(SqlForgeTemplateSql::getExecutorName, template.getExecutorName());
            if (StringUtils.hasText(template.getContext()))
                select.like(SqlForgeTemplateSql::getContext, template.getContext());

            return entityExecutor.run("database", select)
                    .stream()
                    .map(sqlForgeTemplate -> {
                        TemplateSql temp = new TemplateSql();
                        temp.setId(sqlForgeTemplate.getId());
                        temp.setName(sqlForgeTemplate.getName());
                        temp.setDescription(sqlForgeTemplate.getDescription());
                        temp.setExecutorName(sqlForgeTemplate.getExecutorName());
                        temp.setContext(sqlForgeTemplate.getContext());
                        return temp;
                    }).toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
