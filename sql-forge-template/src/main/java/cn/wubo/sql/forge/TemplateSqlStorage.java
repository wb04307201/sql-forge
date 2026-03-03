package cn.wubo.sql.forge;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class TemplateSqlStorage implements ITemplateSqlStorage<TemplateSql> {

    private static final List<TemplateSql> templates = new ArrayList<>();

    @Override
    public void save(TemplateSql template) {
        Optional<TemplateSql> existingMetaData = templates.stream()
                .filter(metaData -> metaData.getId().equals(template.getId()))
                .findFirst();
        if (existingMetaData.isPresent()) {
            int index = templates.indexOf(existingMetaData.get());
            templates.set(index, template);
        } else {
            templates.add(template);
        }
    }

    @Override
    public TemplateSql get(String id) {
        Optional<TemplateSql> existingMetaData = templates.stream()
                .filter(metaData -> metaData.getId().equals(id))
                .findFirst();
        if (existingMetaData.isPresent())
            return existingMetaData.get();
        else
            throw new IllegalArgumentException("模板未找到");
    }

    @Override
    public void remove(String id) {
        Optional<TemplateSql> existingMetaData = templates.stream()
                .filter(metaData -> metaData.getId().equals(id))
                .findFirst();
        if (existingMetaData.isPresent()) {
            templates.remove(existingMetaData.get());
        } else {
            throw new IllegalArgumentException("模板未找到");
        }
    }

    @Override
    public List<TemplateSql> list(TemplateSql template) {
        Stream<TemplateSql> templateStream = templates.stream();
        if (StringUtils.hasText(template.getId()))
            templateStream.filter(item -> item.getId().contains(template.getId()));
        if (StringUtils.hasText(template.getExecutorName()))
            templateStream.filter(item -> item.getExecutorName().equals(template.getExecutorName()));
        if (StringUtils.hasText(template.getContext()))
            templateStream.filter(item -> item.getContext().contains(template.getContext()));

        return templateStream.toList();
    }
}
