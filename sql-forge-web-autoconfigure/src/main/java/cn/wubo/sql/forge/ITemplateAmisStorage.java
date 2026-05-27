package cn.wubo.sql.forge;

import java.util.List;

/**
 * Amis 模板持久化存储接口，定义模板的增删查操作。实现此接口可替换默认的内存存储。
 */
public interface ITemplateAmisStorage<T extends TemplateAmis> {
    void save(T template);
    T get(String id);
    void remove(String id);
    List<T> list(T template);
}
