package cn.wubo.sql.forge.record;

public sealed interface IAllowedRecord permits Delete, Insert, Select, SelectPage, Update {
}
