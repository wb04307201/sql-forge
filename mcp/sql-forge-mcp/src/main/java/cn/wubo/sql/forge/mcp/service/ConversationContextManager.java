package cn.wubo.sql.forge.mcp.service;

import cn.wubo.sql.forge.mcp.model.ConversationContext;
import cn.wubo.sql.forge.mcp.model.QueryIntent;
import cn.wubo.sql.forge.mcp.model.TableSelection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多轮对话上下文管理器
 * 负责管理对话历史，支持基于上下文的连续查询
 */
@Slf4j
@Service
public class ConversationContextManager {

    // 对话上下文缓存（生产环境应使用Redis等外部存储）
    private final Map<String, ConversationContext> contexts = new ConcurrentHashMap<>();

    // 最大历史记录数
    private static final int MAX_HISTORY_SIZE = 50;

    // 上下文过期时间（分钟）
    private static final int CONTEXT_EXPIRY_MINUTES = 30;

    /**
     * 创建或获取会话上下文
     */
    public ConversationContext getOrCreateContext(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        ConversationContext context = contexts.get(sessionId);
        if (context == null) {
            context = ConversationContext.builder()
                    .sessionId(sessionId)
                    .createdAt(LocalDateTime.now())
                    .lastActiveAt(LocalDateTime.now())
                    .build();
            contexts.put(sessionId, context);
            log.debug("创建新会话上下文: {}", sessionId);
        } else {
            // 检查是否过期
            if (isExpired(context)) {
                // 重置上下文
                context = ConversationContext.builder()
                        .sessionId(sessionId)
                        .createdAt(LocalDateTime.now())
                        .lastActiveAt(LocalDateTime.now())
                        .build();
                contexts.put(sessionId, context);
                log.debug("会话上下文已过期，重置: {}", sessionId);
            } else {
                context.setLastActiveAt(LocalDateTime.now());
            }
        }

        return context;
    }

    /**
     * 检查上下文是否过期
     */
    private boolean isExpired(ConversationContext context) {
        if (context.getLastActiveAt() == null) {
            return true;
        }
        return context.getLastActiveAt()
                .plusMinutes(CONTEXT_EXPIRY_MINUTES)
                .isBefore(LocalDateTime.now());
    }

    /**
     * 添加对话轮次
     */
    public void addTurn(String sessionId, String userQuery, String generatedSql,
                        QueryIntent intent, TableSelection tableSelection) {
        ConversationContext context = getOrCreateContext(sessionId);
        context.addTurn(userQuery, generatedSql, intent, tableSelection);

        // 限制历史大小
        if (context.getDialogueHistory().size() > MAX_HISTORY_SIZE) {
            context.getDialogueHistory().remove(0);
        }

        // 更新确认的表
        if (tableSelection != null && tableSelection.getSelectedTables() != null) {
            for (TableSelection.SelectedTable table : tableSelection.getSelectedTables()) {
                context.getConfirmedTables().put(table.getTableName(), table);
            }
        }

        log.debug("添加对话轮次到会话 {}: 用户查询='{}'", sessionId, userQuery);
    }

    /**
     * 记录用户确认
     */
    public void confirmLastTurn(String sessionId, boolean confirmed, String feedback) {
        ConversationContext context = contexts.get(sessionId);
        if (context != null) {
            context.confirmTurn(confirmed, feedback);
        }
    }

    /**
     * 设置澄清状态
     */
    public void setAwaitingClarification(String sessionId, java.util.List<String> questions) {
        ConversationContext context = getOrCreateContext(sessionId);
        context.setAwaitingClarification(questions);
    }

    /**
     * 清除澄清状态
     */
    public void clearClarification(String sessionId) {
        ConversationContext context = contexts.get(sessionId);
        if (context != null) {
            context.clearClarification();
        }
    }

    /**
     * 获取上下文描述（用于提示词构建）
     */
    public String getContextDescription(String sessionId) {
        ConversationContext context = contexts.get(sessionId);
        if (context == null) {
            return "";
        }
        return context.getContextDescription();
    }

    /**
     * 检查是否可以基于上下文继续
     */
    public boolean canContinueFromContext(String sessionId) {
        ConversationContext context = contexts.get(sessionId);
        return context != null && context.canContinueFromContext();
    }

    /**
     * 获取最近确认的表
     */
    public java.util.List<String> getRecentConfirmedTables(String sessionId) {
        ConversationContext context = contexts.get(sessionId);
        return context != null ? context.getRecentConfirmedTables() : java.util.Collections.emptyList();
    }

    /**
     * 设置上下文变量
     */
    public void setContextVariable(String sessionId, String key, Object value) {
        ConversationContext context = getOrCreateContext(sessionId);
        context.getContextVariables().put(key, value);
    }

    /**
     * 获取上下文变量
     */
    public Object getContextVariable(String sessionId, String key) {
        ConversationContext context = contexts.get(sessionId);
        return context != null ? context.getContextVariables().get(key) : null;
    }

    /**
     * 设置自动填充默认值状态
     */
    public void setAutoFilledDefaults(String sessionId, boolean autoFilled) {
        ConversationContext context = getOrCreateContext(sessionId);
        context.setAutoFilledDefaults(autoFilled);
    }

    /**
     * 设置自动填充配置详情
     */
    public void setAutoFilledConfig(String sessionId, java.util.Map<String, Object> config) {
        ConversationContext context = getOrCreateContext(sessionId);
        context.setAutoFilledConfig(config);
    }

    /**
     * 清除会话上下文
     */
    public void clearContext(String sessionId) {
        contexts.remove(sessionId);
        log.debug("清除会话上下文: {}", sessionId);
    }

    /**
     * 清除所有过期上下文
     */
    public void clearExpiredContexts() {
        contexts.entrySet().removeIf(entry -> isExpired(entry.getValue()));
        log.info("已清除过期会话上下文，当前剩余: {} 个", contexts.size());
    }

    /**
     * 获取当前会话数量
     */
    public int getActiveSessionCount() {
        return contexts.size();
    }

    /**
     * 获取会话统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("activeSessions", contexts.size());
        stats.put("maxHistorySize", MAX_HISTORY_SIZE);
        stats.put("contextExpiryMinutes", CONTEXT_EXPIRY_MINUTES);

        // 计算平均对话轮次
        int totalTurns = contexts.values().stream()
                .mapToInt(c -> c.getDialogueHistory().size())
                .sum();
        stats.put("averageTurnsPerSession",
                contexts.isEmpty() ? 0 : (double) totalTurns / contexts.size());

        return stats;
    }
}
