package cn.eubo.sql.forge.entity.base;

public abstract class AbstractEntity<T, R, C extends AbstractEntity<T, R, C>> extends AbstractWhere<T, R, C> {

    protected T entity;

    protected AbstractEntity(Class<T> entityClass) {
        super(entityClass);
    }

    public C entity(T entity){
        this.entity = entity;
        return typedThis;
    }




























}
