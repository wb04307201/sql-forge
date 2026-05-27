package cn.wubo.sql.forge;

/**
 * SQL 生成模式枚举，控制模板参数是使用 ? 占位符（WITH_PLACEHOLDERS）还是内联字面值（WITH_VALUES）。
 */
public enum GenerationSqlMode {
    WITH_PLACEHOLDERS,  // 使用占位符（默认行为）
    WITH_VALUES         // 直接拼接值
}

