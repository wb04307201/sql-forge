package cn.wubo.sql.forge;

import cn.wubo.sql.forge.Entity;
import cn.wubo.sql.forge.EntityExecutor;
import cn.wubo.sql.forge.entity.EntitySelect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class CustomTemplateAmisStorage implements ITemplateAmisStorage<TemplateAmis> {

    private final EntityExecutor entityExecutor;

    public CustomTemplateAmisStorage(EntityExecutor entityExecutor) {
        this.entityExecutor = entityExecutor;
    }

    @Override
    public void save(TemplateAmis template) {
        try {
            List list = entityExecutor.run("database", Entity.select(SqlForgeTemplateAmis.class).eq(SqlForgeTemplateAmis::getId, template.getId()));
            if (list.isEmpty()) {
                entityExecutor.run("database", Entity.insert(SqlForgeTemplateAmis.class)
                        .set(SqlForgeTemplateAmis::getName, template.getName())
                        .set(SqlForgeTemplateAmis::getDescription, template.getDescription())
                        .set(SqlForgeTemplateAmis::getContext, template.getContext())
                        .set(SqlForgeTemplateAmis::getId, template.getId()));
            } else {
                entityExecutor.run("database", Entity.update(SqlForgeTemplateAmis.class)
                        .set(SqlForgeTemplateAmis::getName, template.getName())
                        .set(SqlForgeTemplateAmis::getDescription, template.getDescription())
                        .set(SqlForgeTemplateAmis::getContext, template.getContext())
                        .eq(SqlForgeTemplateAmis::getId, template.getId()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public TemplateAmis get(String id) {
        try {
            List<SqlForgeTemplateAmis> list = entityExecutor.run("database", Entity.select(SqlForgeTemplateAmis.class).eq(SqlForgeTemplateAmis::getId, id));
            if (list.isEmpty()) {
                throw new RuntimeException("模板未找到");
            } else {
                SqlForgeTemplateAmis sqlForgeTemplate = list.get(0);
                TemplateAmis template = new TemplateAmis();
                template.setId(sqlForgeTemplate.getId());
                template.setName(sqlForgeTemplate.getName());
                template.setDescription(sqlForgeTemplate.getDescription());
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
            entityExecutor.run("database", Entity.delete(SqlForgeTemplateAmis.class).eq(SqlForgeTemplateAmis::getId, id));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<TemplateAmis> list(TemplateAmis template) {
        try {
            EntitySelect<SqlForgeTemplateAmis> select = Entity.select(SqlForgeTemplateAmis.class);
            if (StringUtils.hasText(template.getId()))
                select.like(SqlForgeTemplateAmis::getId, template.getId());
            if (StringUtils.hasText(template.getName()))
                select.like(SqlForgeTemplateAmis::getName, template.getName());
            if (StringUtils.hasText(template.getDescription()))
                select.like(SqlForgeTemplateAmis::getDescription, template.getDescription());
            if (StringUtils.hasText(template.getContext()))
                select.like(SqlForgeTemplateAmis::getContext, template.getContext());

            return entityExecutor.run("database", select)
                    .stream()
                    .map(sqlForgeTemplate -> {
                        TemplateAmis temp = new TemplateAmis();
                        temp.setId(sqlForgeTemplate.getId());
                        temp.setName(sqlForgeTemplate.getName());
                        temp.setDescription(sqlForgeTemplate.getDescription());
                        temp.setContext(sqlForgeTemplate.getContext());
                        return temp;
                    }).toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
