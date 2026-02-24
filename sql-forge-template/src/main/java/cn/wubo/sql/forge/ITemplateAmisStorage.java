package cn.wubo.sql.forge;

import java.util.List;

public interface ITemplateAmisStorage<T extends TemplateAmis> {
    void save(T template);
    T get(String id);
    void remove(String id);
    List<T> list(T template);
}
