package cn.wubo.sql.forge.record;

/**
 * 允许的 Record 操作标记接口（sealed），限制仅 {@link Delete}、{@link Insert}、
 * {@link Select}、{@link SelectPage}、{@link Update} 可作为操作类型。
 */
public sealed interface IAllowedRecord permits Delete, Insert, Select, SelectPage, Update {
}
