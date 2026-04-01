package cn.wubo.sql.forge.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL验证结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlValidationResult {

    /**
     * 是否有效
     */
    private boolean valid;

    /**
     * 验证类型
     */
    private ValidationType validationType;

    /**
     * 错误消息列表
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * 警告消息列表
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * 优化建议
     */
    @Builder.Default
    private List<String> suggestions = new ArrayList<>();

    /**
     * 修正后的SQL
     */
    private String correctedSql;

    /**
     * 验证详情
     */
    private String details;

    /**
     * 验证类型枚举
     */
    public enum ValidationType {
        SYNTAX("语法验证"),
        SEMANTIC("语义验证"),
        PERFORMANCE("性能验证"),
        SECURITY("安全性验证"),
        COMPLETE("完整性验证");

        private final String description;

        ValidationType(String description) {
            this.description = description;
        }
    }

    /**
     * 添加错误
     */
    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }

    /**
     * 添加警告
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    /**
     * 添加建议
     */
    public void addSuggestion(String suggestion) {
        this.suggestions.add(suggestion);
    }

    /**
     * 获取所有问题的总结
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        if (!errors.isEmpty()) {
            sb.append("错误:\n");
            errors.forEach(e -> sb.append("  - ").append(e).append("\n"));
        }
        if (!warnings.isEmpty()) {
            sb.append("警告:\n");
            warnings.forEach(w -> sb.append("  - ").append(w).append("\n"));
        }
        if (!suggestions.isEmpty()) {
            sb.append("建议:\n");
            suggestions.forEach(s -> sb.append("  - ").append(s).append("\n"));
        }
        return sb.toString();
    }

    /**
     * 创建有效的验证结果
     */
    public static SqlValidationResult valid() {
        return SqlValidationResult.builder()
                .valid(true)
                .validationType(ValidationType.COMPLETE)
                .build();
    }

    /**
     * 创建无效的验证结果
     */
    public static SqlValidationResult invalid(ValidationType type, String error) {
        SqlValidationResult result = SqlValidationResult.builder()
                .valid(false)
                .validationType(type)
                .build();
        result.addError(error);
        return result;
    }
}
