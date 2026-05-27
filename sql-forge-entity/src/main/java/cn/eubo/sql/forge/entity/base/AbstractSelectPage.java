package cn.eubo.sql.forge.entity.base;

import cn.wubo.sql.forge.record.base.Page;

/**
 * 分页 SELECT 抽象构建器，在 AbstractSelect 基础上增加 page 方法。
 *
 * @param <T> 实体类型
 * @param <R> 执行结果类型
 * @param <C> 具体子类类型
 */
public abstract class AbstractSelectPage<T, R, C extends AbstractSelectPage<T, R, C>> extends AbstractSelect<T, R, C> {
    /** 分页参数，默认第 0 页、每页 10 条 */
    protected Page page = new Page(0,10);

    /**
     * 构造方法。
     *
     * @param entityClass 实体类对象
     */
    protected AbstractSelectPage(Class<T> entityClass) {
        super(entityClass);
    }

    /**
     * 设置分页参数。
     *
     * @param pageIndex 页码（从 0 开始）
     * @param pageSize  每页条数
     * @return 当前构建器
     */
    public C page(Integer pageIndex, Integer pageSize) {
        this.page = new Page(pageIndex, pageSize);
        return typedThis;
    }
}
