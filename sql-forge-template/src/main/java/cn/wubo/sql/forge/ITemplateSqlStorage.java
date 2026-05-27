package cn.wubo.sql.forge;

import java.util.List;

/**
 * SQL 模板持久化存储接口，定义模板的增删查操作。实现此接口可替换默认的内存存储。
 *
 * @param <T> 模板类型，必须继承 {@link TemplateSql}
 */
public interface ITemplateSqlStorage<T extends TemplateSql> {
    /**
     * 保存或更新模板。
     *
     * @param template 要保存的模板对象
     */
    void save(T template);

    /**
     * 根据 ID 获取模板。
     *
     * @param id 模板唯一标识
     * @return 模板对象，未找到时返回 {@code null}
     */
    T get(String id);

    /**
     * 根据 ID 删除模板。
     *
     * @param id 模板唯一标识
     */
    void remove(String id);

    /**
     * 根据条件查询模板列表。
     *
     * @param template 查询条件，非空字段作为过滤条件
     * @return 匹配的模板列表
     */
    List<T> list(T template);
}
