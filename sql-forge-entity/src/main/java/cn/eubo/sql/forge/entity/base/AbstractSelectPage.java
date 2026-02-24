package cn.eubo.sql.forge.entity.base;

import cn.wubo.sql.forge.record.base.Page;

public abstract class AbstractSelectPage<T, R, C extends AbstractSelectPage<T, R, C>> extends AbstractSelect<T, R, C> {
    protected Page page = new Page(0,10);

    protected AbstractSelectPage(Class<T> entityClass) {
        super(entityClass);
    }

    public C page(Integer pageIndex, Integer pageSize) {
        this.page = new Page(pageIndex, pageSize);
        return typedThis;
    }
}
