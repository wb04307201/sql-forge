package cn.wubo.sql.forge.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话上下文
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContext {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 会话创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveAt;

    /**
     * 对话历史
     */
    @Builder.Default
    private List<DialogueTurn> dialogueHistory = new ArrayList<>();

    /**
     * 已确认的表（用户确认使用的表）
     */
    @Builder.Default
    private Map<String, TableSelection.SelectedTable> confirmedTables = new HashMap<>();

    /**
     * 已确认的字段
     */
    @Builder.Default
    private Map<String, String> confirmedColumns = new HashMap<>();

    /**
     * 当前查询意图
     */
    private QueryIntent currentIntent;

    /**
     * 当前选择的表
     */
    private TableSelection currentTableSelection;

    /**
     * 上下文变量（用于存储中间结果）
     */
    @Builder.Default
    private Map<String, Object> contextVariables = new HashMap<>();

    /**
     * 是否处于澄清状态
     */
    private boolean awaitingClarification;

    /**
     * 澄清问题
     */
    private List<String> clarificationQuestions;

    /**
     * 是否自动填充了默认值（用于轻微模糊意图）
     */
    @Builder.Default
    private boolean autoFilledDefaults = false;

    /**
     * 自动填充的配置详情
     */
    private Map<String, Object> autoFilledConfig;

    /**
     * 对话轮次
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DialogueTurn {
        private String userQuery;
        private String generatedSql;
        private QueryIntent intent;
        private TableSelection tableSelection;
        private boolean userConfirmed;
        private String userFeedback;
        private LocalDateTime timestamp;
    }

    /**
     * 添加一轮对话
     */
    public void addTurn(String userQuery, String generatedSql, QueryIntent intent, TableSelection tableSelection) {
        DialogueTurn turn = DialogueTurn.builder()
                .userQuery(userQuery)
                .generatedSql(generatedSql)
                .intent(intent)
                .tableSelection(tableSelection)
                .timestamp(LocalDateTime.now())
                .build();
        this.dialogueHistory.add(turn);
        this.lastActiveAt = LocalDateTime.now();
    }

    /**
     * 确认用户的反馈
     */
    public void confirmTurn(boolean confirmed, String feedback) {
        if (!dialogueHistory.isEmpty()) {
            DialogueTurn lastTurn = dialogueHistory.get(dialogueHistory.size() - 1);
            lastTurn.setUserConfirmed(confirmed);
            lastTurn.setUserFeedback(feedback);
        }
    }

    /**
     * 设置澄清状态
     */
    public void setAwaitingClarification(List<String> questions) {
        this.awaitingClarification = true;
        this.clarificationQuestions = questions;
    }

    /**
     * 清除澄清状态
     */
    public void clearClarification() {
        this.awaitingClarification = false;
        this.clarificationQuestions = null;
    }

    /**
     * 检查是否可以基于上下文继续查询
     */
    public boolean canContinueFromContext() {
        return !confirmedTables.isEmpty() && !dialogueHistory.isEmpty();
    }

    /**
     * 获取最近的确认表
     */
    public List<String> getRecentConfirmedTables() {
        return new ArrayList<>(confirmedTables.keySet());
    }

    /**
     * 获取上下文描述（用于提示词构建）
     */
    public String getContextDescription() {
        if (dialogueHistory.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("上下文信息:\n");
        if (!confirmedTables.isEmpty()) {
            sb.append("- 已确认的表: ").append(String.join(", ", confirmedTables.keySet())).append("\n");
        }
        if (currentIntent != null) {
            sb.append("- 当前意图: ").append(currentIntent.getIntentDescription()).append("\n");
        }
        return sb.toString();
    }
}
