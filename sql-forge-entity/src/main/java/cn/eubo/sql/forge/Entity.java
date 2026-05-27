package cn.eubo.sql.forge;

import cn.eubo.sql.forge.entity.*;
import lombok.experimental.UtilityClass;

/**
 * 实体操作工具类，提供对实体对象进行数据库操作的便捷方法。
 * 包含增删改查等基本数据库操作的构建器。
 */
@UtilityClass
public class Entity {


    /**
     * 创建一个实体删除操作对象。
     *
     * @param <T>         实体类型参数
     * @param entityClass 要删除的实体类的Class对象
     * @return EntityDelete类型的删除操作对象，用于构建和执行删除操作
     */
    public <T> EntityDelete<T> delete(Class<T> entityClass) {
        return new EntityDelete<>(entityClass);
    }

    /**
     * 根据对象创建删除操作实例。
     *
     * @param <T>       要删除的对象类型
     * @param deleteObj 要删除的对象实例，用于获取类型信息和作为删除条件
     * @return EntityDeleteByKey类型的删除操作实例，用于构建和执行删除操作
     */
    public <T> EntityDeleteByKey<T> delete(T deleteObj) {
        return new EntityDeleteByKey<>((Class<T>) deleteObj.getClass()).entity(deleteObj);
    }

    /**
     * 创建一个实体插入操作对象。
     *
     * @param <T>         实体类型参数
     * @param entityClass 要插入的实体类的Class对象
     * @return 返回EntityInsert类型的插入操作对象，用于构建和执行插入操作
     */
    public <T> EntityInsert<T> insert(Class<T> entityClass) {
        return new EntityInsert<>(entityClass);
    }

    /**
     * 创建一个实体查询选择器。
     *
     * @param <T>         实体类型参数
     * @param entityClass 要查询的实体类的Class对象
     * @return 返回指定实体类型的EntitySelect查询器实例
     */
    public <T> EntitySelect<T> select(Class<T> entityClass) {
        return new EntitySelect<>(entityClass);
    }

    /**
     * 创建一个实体分页查询对象。
     *
     * @param <T>         实体类型参数
     * @param entityClass 实体类的Class对象，用于指定要查询的实体类型
     * @return 返回指定实体类型的分页查询对象
     */
    public <T> EntitySelectPage<T> selectPage(Class<T> entityClass) {
        return new EntitySelectPage<>(entityClass);
    }

    /**
     * 创建一个实体更新操作对象。
     *
     * @param <T>         实体类型参数
     * @param entityClass 要更新的实体类的Class对象
     * @return 返回指定实体类型的EntityUpdate操作对象，用于构建和执行更新操作
     */
    public <T> EntityUpdate<T> update(Class<T> entityClass) {
        return new EntityUpdate<>(entityClass);
    }

    /**
     * 保存实体对象（新增或更新）。
     *
     * @param <T>     实体对象的类型
     * @param saveObj 需要保存的实体对象
     * @return EntitySave对象，用于进一步处理保存操作
     */
    public <T> EntitySave<T> save(T saveObj) {
        return new EntitySave<>((Class<T>) saveObj.getClass()).entity(saveObj);
    }
}
