package cn.wubo.sql.forge.mcp.service;

import cn.wubo.sql.forge.mcp.enums.DialectType;
import cn.wubo.sql.forge.mcp.model.ConversationContext;
import cn.wubo.sql.forge.mcp.model.ConversationContext.DialogueTurn;
import cn.wubo.sql.forge.mcp.model.QueryIntent;
import cn.wubo.sql.forge.mcp.model.SqlValidationResult;
import cn.wubo.sql.forge.mcp.model.TableSelection;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自然语言转SQL服务 - 深度优化版本
 * <p>
 * 功能特性：
 * 1. 意图识别：自动识别查询类型（简单查询、聚合查询、分组查询、JOIN查询等）
 * 2. 智能表选择：根据用户查询语义选择相关表并评分
 * 3. 方言适配：支持MySQL、PostgreSQL、Oracle、SQL Server、H2、SQLite等数据库
 * 4. SQL验证：语法、语义、性能、安全性多维度验证
 * 5. 多轮对话：支持会话上下文，保持查询上下文连续性
 * 6. 模糊意图处理：自动识别模糊查询，生成澄清问题
 * <p>
 * 使用方法：
 * 1. 基础查询：直接调用 NL2SQL("查询所有用户信息")
 * 2. 多轮对话：调用 NL2SQLWithContext(content, sessionId)
 * 3. 澄清反馈：调用 ConfirmClarification(sessionId, feedback)
 * 4. 切换方言：调用 SetDialect("POSTGRESQL")
 * <p>
 * 依赖说明：
 * - 不依赖 sql-forge-core 或 sql-forge-spring-boot-autoconfigure
 * - 使用 RestClient 调用 sql-forge API 获取元数据
 * - 使用 Spring AI 调用大模型生成SQL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NL2SQLService {

    private final ChatClient chatClient;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;
    private final String executorName;

    // 核心服务组件
    private final IntentRecognizer intentRecognizer;
    private final TableSelector tableSelector;
    private final SqlValidator sqlValidator;
    private final DialectAdapter dialectAdapter;
    private final ConversationContextManager contextManager;
    private final AmbiguityResolver ambiguityResolver;

    // 当前数据库方言
    private DialectType currentDialect = DialectType.MYSQL;

    // 默认配置（用于模糊意图查询）
    private final String defaultSortField;
    private final String defaultSortDirection;
    private final int defaultLimit;
    private final double ambiguityThreshold;

    @Autowired
    public NL2SQLService(
            @Value("${sql-forge.api.url:http://localhost:8081}") String apiBaseUrl,
            @Value("${sql-forge.api.executor:database}") String executorName,
            @Value("${sql-forge.dialect:auto}") String dialect,
            @Value("${sql-forge.default.sort-field:id}") String defaultSortField,
            @Value("${sql-forge.default.sort-direction:DESC}") String defaultSortDirection,
            @Value("${sql-forge.default.limit:10}") int defaultLimit,
            @Value("${sql-forge.default.ambiguity-threshold:0.7}") double ambiguityThreshold,
            IntentRecognizer intentRecognizer,
            TableSelector tableSelector,
            SqlValidator sqlValidator,
            DialectAdapter dialectAdapter,
            ConversationContextManager contextManager,
            AmbiguityResolver ambiguityResolver,
            @Autowired(required = false) ChatClient chatClient,
            @Autowired(required = false) RestClient restClient) {
        this.apiBaseUrl = apiBaseUrl;
        this.executorName = executorName;
        this.intentRecognizer = intentRecognizer;
        this.tableSelector = tableSelector;
        this.sqlValidator = sqlValidator;
        this.dialectAdapter = dialectAdapter;
        this.contextManager = contextManager;
        this.ambiguityResolver = ambiguityResolver;
        this.defaultSortField = defaultSortField;
        this.defaultSortDirection = defaultSortDirection;
        this.defaultLimit = defaultLimit;
        this.ambiguityThreshold = ambiguityThreshold;

        // 初始化ObjectMapper
        this.objectMapper = new ObjectMapper();

        // 直接使用注入的 ChatClient
        this.chatClient = chatClient;

        // 构建RestClient
        this.restClient = restClient;

        // 解析方言配置
        this.currentDialect = DialectType.fromDatabaseType(dialect);

        log.info("NL2SQLService初始化完成，默认方言: {}, 模糊阈值: {}, 默认限制: {}, 默认排序: {} {}",
                currentDialect.getDisplayName(), ambiguityThreshold, defaultLimit, defaultSortField, defaultSortDirection);
    }

    // ==================== 核心工具方法 ====================

    /**
     * 将自然语言转换为SQL（标准入口）
     *
     * @param content 自然语言查询描述
     * @return 生成的SQL语句
     */
    @Tool(name = "NL2SQL", description = "将自然语言描述的需求转换成SQL查询语句，支持复杂查询、聚合、分组、多表关联等场景")
    public String NL2SQL(@ToolParam(description = "自然语言描述的需求，例如：查询所有年龄大于30岁的用户，按姓名升序排列") String content) {
        return nl2sqlWithContext(content, null);
    }

    /**
     * 将自然语言转换为SQL（带会话上下文）
     *
     * @param content   自然语言查询描述
     * @param sessionId 会话ID，用于多轮对话（可为空）
     * @return 生成的SQL语句或澄清提示
     */
    @Tool(name = "NL2SQLWithContext", description = "使用会话上下文将自然语言转换成SQL，支持多轮对话记忆")
    public String nl2sqlWithContext(
            @ToolParam(description = "自然语言描述的需求") String content,
            @ToolParam(description = "会话ID，用于保持对话上下文（可选）") String sessionId) {

        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取或创建会话上下文
            ConversationContext context = contextManager.getOrCreateContext(sessionId);
            log.debug("处理查询，会话ID: {}, 查询内容: {}", sessionId, content);

            // 2. 意图识别
            QueryIntent intent = intentRecognizer.recognize(content);
            context.setCurrentIntent(intent);
            log.debug("识别到意图: {}, 置信度: {}", intent.getPrimaryIntent(), intent.getConfidence());

            // 3. 处理模糊意图
            boolean isSlightlyAmbiguous = intent.getConfidence() < ambiguityThreshold && intent.getConfidence() >= 0.4;
            boolean isHighlyAmbiguous = intent.getConfidence() < 0.4;

            if (isHighlyAmbiguous) {
                // 高度模糊，需要澄清
                String schemaInfo = getMetaDataFromApi();
                List<AmbiguityResolver.AmbiguityInterpretation> interpretations =
                        ambiguityResolver.resolveAmbiguity(content, schemaInfo, intent);
                contextManager.setAwaitingClarification(sessionId,
                        interpretations.stream()
                                .flatMap(i -> i.getClarificationQuestions() != null ?
                                        i.getClarificationQuestions().stream() :
                                        java.util.stream.Stream.empty())
                                .distinct()
                                .collect(Collectors.toList()));
                return formatAmbiguousResponse(interpretations);
            } else if (isSlightlyAmbiguous) {
                // 轻微模糊，自动填充默认值并继续
                log.info("检测到轻微模糊意图(置信度: {})，将自动应用默认配置继续生成SQL", intent.getConfidence());
                contextManager.setAutoFilledDefaults(sessionId, true);
            }

            // 4. 获取数据库元数据
            String schemaInfo = getMetaDataFromApi();
            if ("[]".equals(schemaInfo)) {
                log.warn("获取元数据失败或为空");
            }

            // 5. 智能表选择
            TableSelection tableSelection = tableSelector.selectTables(content, schemaInfo, intent, context);
            context.setCurrentTableSelection(tableSelection);
            log.debug("选择表数量: {}, 整体相关度: {}",
                    tableSelection.getSelectedTables() != null ? tableSelection.getSelectedTables().size() : 0,
                    tableSelection.getTotalScore());

            // 6. 构建增强提示词
            String enhancedPrompt = buildEnhancedPrompt(content, schemaInfo, intent, tableSelection, context);

            // 7. 调用大模型生成SQL
            String rawSql = callLlm(enhancedPrompt);
            log.debug("原始SQL: {}", rawSql);

            // 8. 验证SQL
            SqlValidationResult validationResult = sqlValidator.validate(rawSql);
            log.debug("SQL验证结果: valid={}, errors={}", validationResult.isValid(), validationResult.getErrors().size());

            // 9. 自动修正（如果需要）
            String finalSql = rawSql;
            if (!validationResult.isValid()) {
                String correctedSql = sqlValidator.tryCorrect(rawSql);
                SqlValidationResult reValidation = sqlValidator.validate(correctedSql);
                if (reValidation.isValid()) {
                    finalSql = correctedSql;
                    validationResult = reValidation;
                    log.info("SQL已自动修正");
                }
            }

            // 10. 方言适配
            finalSql = dialectAdapter.adapt(finalSql, currentDialect);

            // 11. 记录到对话历史
            contextManager.addTurn(sessionId, content, finalSql, intent, tableSelection);
            contextManager.clearClarification(sessionId);

            // 12. 格式化响应
            long costTime = System.currentTimeMillis() - startTime;
            log.info("NL2SQL转换完成，耗时: {}ms", costTime);

            return formatResponse(finalSql, validationResult);

        } catch (Exception e) {
            log.error("NL2SQL转换失败", e);
            return "生成SQL失败: " + e.getMessage();
        }
    }

    /**
     * 处理澄清反馈
     *
     * @param sessionId 会话ID
     * @param feedback  用户澄清反馈
     * @return 基于澄清重新生成的SQL
     */
    @Tool(name = "ConfirmClarification", description = "处理用户对歧义查询的澄清反馈，例如'前10条'、'按销量排序'等")
    public String confirmClarification(
            @ToolParam(description = "会话ID") String sessionId,
            @ToolParam(description = "用户的澄清反馈") String feedback) {

        try {
            ConversationContext context = contextManager.getOrCreateContext(sessionId);

            if (!context.isAwaitingClarification()) {
                return "当前没有待澄清的问题，请直接输入您的查询需求。";
            }

            log.info("处理澄清反馈，会话ID: {}, 反馈: {}", sessionId, feedback);

            // 基于反馈更新意图并重新生成
            String originalQuery = context.getCurrentIntent() != null ?
                    context.getCurrentIntent().getOriginalQuery() : "";
            String enrichedQuery = originalQuery + " " + feedback;

            return nl2sqlWithContext(enrichedQuery, sessionId);

        } catch (Exception e) {
            log.error("处理澄清反馈失败", e);
            return "处理澄清反馈失败: " + e.getMessage();
        }
    }

    /**
     * 执行SQL并返回结果
     *
     * @param sql 要执行的SQL语句
     * @return 查询结果
     */
    @Tool(name = "ExecuteSQL", description = "执行SQL查询并返回结果集")
    public String executeSQL(@ToolParam(description = "要执行的SQL语句") String sql) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sql", sql);

            log.info("执行SQL: {}", sql);

            String result = restClient.post()
                    .uri("/sql/forge/api/database/execute?executorName={executor}", executorName)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            log.info("SQL执行成功");
            return result;

        } catch (HttpClientErrorException e) {
            String errorMsg = "执行SQL失败，状态码: " + e.getStatusCode() + "，响应: " + e.getResponseBodyAsString();
            log.error(errorMsg);
            return errorMsg;
        } catch (Exception e) {
            String errorMsg = "执行SQL失败: " + e.getMessage();
            log.error(errorMsg, e);
            return errorMsg;
        }
    }

    /**
     * 获取对话历史
     */
    @Tool(name = "GetConversationHistory", description = "获取当前会话的对话历史记录")
    public String getConversationHistory(@ToolParam(description = "会话ID") String sessionId) {
        try {
            ConversationContext context = contextManager.getOrCreateContext(sessionId);
            List<DialogueTurn> history = context.getDialogueHistory();

            if (history.isEmpty()) {
                return "当前会话暂无对话历史。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("【对话历史】会话ID: ").append(sessionId).append("\n");
            sb.append("共 ").append(history.size()).append(" 轮对话\n\n");

            for (int i = 0; i < history.size(); i++) {
                DialogueTurn turn = history.get(i);
                sb.append("━━━━━━━━ 第 ").append(i + 1).append(" 轮 ━━━━━━━━\n");
                sb.append("  用户：").append(turn.getUserQuery()).append("\n");
                sb.append("  SQL：").append(turn.getGeneratedSql()).append("\n");
                if (turn.getIntent() != null) {
                    sb.append("  意图：").append(turn.getIntent().getIntentDescription()).append("\n");
                }
                if (turn.getUserFeedback() != null) {
                    sb.append("  反馈：").append(turn.getUserFeedback()).append("\n");
                }
                sb.append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("获取对话历史失败", e);
            return "获取对话历史失败: " + e.getMessage();
        }
    }

    /**
     * 切换数据库方言
     *
     * @param dialect 数据库类型，可选值：MYSQL, POSTGRESQL, ORACLE, SQL_SERVER, H2, SQLITE, STANDARD_SQL
     */
    @Tool(name = "SetDialect", description = "设置目标数据库方言类型，支持：MYSQL, POSTGRESQL, ORACLE, SQL_SERVER, H2, SQLITE, STANDARD_SQL")
    public String setDialect(@ToolParam(description = "数据库方言类型") String dialect) {
        try {
            DialectType newDialect = DialectType.fromDatabaseType(dialect);
            this.currentDialect = newDialect;
            log.info("方言已切换: {}", newDialect.getDisplayName());
            return "✓ 方言已切换为: " + newDialect.getDisplayName();
        } catch (Exception e) {
            log.error("切换方言失败", e);
            return "切换方言失败: " + e.getMessage();
        }
    }

    /**
     * 获取当前方言
     */
    public String getCurrentDialect() {
        return currentDialect.getDisplayName();
    }

    /**
     * 清除会话上下文
     */
    @Tool(name = "ClearSession", description = "清除指定会话的上下文")
    public String clearSession(@ToolParam(description = "会话ID") String sessionId) {
        contextManager.clearContext(sessionId);
        return "✓ 会话上下文已清除";
    }

    /**
     * 获取系统统计信息
     */
    @Tool(name = "GetSystemStats", description = "获取系统统计信息，包括会话数、平均对话轮次等")
    public String getSystemStats() {
        Map<String, Object> stats = contextManager.getStatistics();
        StringBuilder sb = new StringBuilder();
        sb.append("【系统统计】\n");
        sb.append("- 活跃会话数：").append(stats.get("activeSessions")).append("\n");
        sb.append("- 最大历史记录数：").append(stats.get("maxHistorySize")).append("\n");
        sb.append("- 上下文有效期：").append(stats.get("contextExpiryMinutes")).append(" 分钟\n");
        sb.append("- 平均对话轮次：").append(String.format("%.1f", stats.get("averageTurnsPerSession"))).append("\n");
        return sb.toString();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 从API获取元数据信息
     */
    private String getMetaDataFromApi() {
        try {
            return restClient.get()
                    .uri("/sql/forge/api/database/metaDataTables?executorName={executor}", executorName)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException e) {
            log.error("获取元数据失败，状态码: {}", e.getStatusCode());
            return "[]";
        } catch (Exception e) {
            log.error("调用元数据API失败", e);
            return "[]";
        }
    }

    /**
     * 调用大模型生成SQL
     */
    private String callLlm(String prompt) {
        if (chatClient == null) {
            log.warn("ChatClient未配置，使用降级模式");
            return generateFallbackSql(prompt);
        }

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return extractSql(response);

        } catch (Exception e) {
            log.error("调用大模型失败", e);
            return generateFallbackSql(prompt);
        }
    }

    /**
     * 提取SQL语句
     */
    private String extractSql(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "";
        }

        String trimmed = response.trim();

        // 移除markdown代码块
        if (trimmed.startsWith("```sql")) {
            trimmed = trimmed.substring(5);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        return trimmed.trim();
    }

    /**
     * 生成降级SQL
     */
    private String generateFallbackSql(String prompt) {
        // 基于提示词内容生成简单的SQL
        return "-- 请检查系统配置，大模型服务不可用";
    }

    /**
     * 构建增强提示词
     */
    private String buildEnhancedPrompt(String userQuery, String schemaInfo,
                                       QueryIntent intent, TableSelection tableSelection,
                                       ConversationContext context) {
        StringBuilder prompt = new StringBuilder();

        // 系统角色
        prompt.append("你是专业的SQL生成助手。请根据用户需求和数据库Schema，生成准确的SQL查询语句。\n\n");

        // 数据库方言
        prompt.append("【数据库方言】").append(currentDialect.getDisplayName()).append("\n\n");

        // 意图信息
        prompt.append("【识别到的查询意图】\n");
        prompt.append("- 类型：").append(intent.getIntentDescription()).append("\n");
        prompt.append("- 置信度：").append(String.format("%.0f%%", intent.getConfidence() * 100)).append("\n");

        // 检查是否需要自动填充默认值
        boolean isAmbiguous = intent.getConfidence() < ambiguityThreshold || intent.isAmbiguous();
        boolean needsAutoFill = context != null && context.isAutoFilledDefaults();
        QueryIntent.SortRequirement effectiveSort = intent.getSortRequirement();
        QueryIntent.PaginationRequirement effectivePagination = intent.getPagination();

        // 记录自动填充的配置
        Map<String, Object> autoFilledConfig = new HashMap<>();

        if (isAmbiguous || needsAutoFill) {
            if (needsAutoFill) {
                prompt.append("- ⚠️ 检测到轻微模糊意图，已自动填充缺失参数\n");
            } else {
                prompt.append("- ⚠️ 检测到模糊意图，将自动应用默认配置\n");
            }

            // 自动添加默认排序
            boolean autoFilledSort = false;
            if (effectiveSort == null || effectiveSort.getOrderByFields() == null || effectiveSort.getOrderByFields().isEmpty()) {
                effectiveSort = QueryIntent.SortRequirement.builder()
                        .orderByFields(List.of(defaultSortField))
                        .descending("DESC".equalsIgnoreCase(defaultSortDirection))
                        .build();
                intent.setSortRequirement(effectiveSort);
                prompt.append("- 默认排序：").append(defaultSortField).append(" (").append(defaultSortDirection).append(")\n");
                autoFilledConfig.put("sortField", defaultSortField);
                autoFilledConfig.put("sortDirection", defaultSortDirection);
                autoFilledSort = true;
            }

            // 自动添加默认限制
            boolean autoFilledLimit = false;
            if (effectivePagination == null || effectivePagination.getLimit() <= 0) {
                effectivePagination = QueryIntent.PaginationRequirement.builder()
                        .page(1)
                        .pageSize(defaultLimit)
                        .limit(defaultLimit)
                        .offset(0)
                        .build();
                intent.setPagination(effectivePagination);
                prompt.append("- 默认限制：").append(defaultLimit).append(" 条\n");
                autoFilledConfig.put("limit", defaultLimit);
                autoFilledLimit = true;
            }

            // 记录到上下文
            if (needsAutoFill && context != null && context.getSessionId() != null) {
                contextManager.setAutoFilledConfig(context.getSessionId(), autoFilledConfig);
            }
        }

        if (intent.getAggregateFields() != null && !intent.getAggregateFields().isEmpty()) {
            prompt.append("- 聚合字段：").append(String.join(", ", intent.getAggregateFields())).append("\n");
        }
        if (intent.getGroupByFields() != null && !intent.getGroupByFields().isEmpty()) {
            prompt.append("- 分组字段：").append(String.join(", ", intent.getGroupByFields())).append("\n");
        }
        if (effectiveSort != null && effectiveSort.getOrderByFields() != null && !effectiveSort.getOrderByFields().isEmpty()) {
            prompt.append("- 排序：").append(String.join(", ", effectiveSort.getOrderByFields()));
            prompt.append(effectiveSort.isDescending() ? " (降序)\n" : " (升序)\n");
        }
        if (effectivePagination != null && effectivePagination.getLimit() > 0) {
            prompt.append("- 分页：限制 ").append(effectivePagination.getLimit()).append(" 条");
            if (effectivePagination.getOffset() > 0) {
                prompt.append(", 偏移 ").append(effectivePagination.getOffset());
            }
            prompt.append("\n");
        }
        prompt.append("\n");

        // 表选择信息
        prompt.append("【建议使用的表】\n");
        if (tableSelection != null && tableSelection.getSelectedTables() != null
                && !tableSelection.getSelectedTables().isEmpty()) {
            for (TableSelection.SelectedTable table : tableSelection.getSelectedTables()) {
                prompt.append("- ").append(table.getTableName());
                if (table.getRemarks() != null && !table.getRemarks().isEmpty()) {
                    prompt.append(" [" + table.getRemarks() + "]");
                }
                prompt.append(" [相关度: ").append(String.format("%.0f%%", table.getRelevanceScore() * 100)).append("]\n");

                if (table.getRelevantColumns() != null && !table.getRelevantColumns().isEmpty()) {
                    prompt.append("  相关列: ");
                    prompt.append(table.getRelevantColumns().stream()
                            .map(c -> c.getColumnName() + "(" + c.getUsage() + ")")
                            .collect(Collectors.joining(", ")));
                    prompt.append("\n");
                }
            }
        } else {
            prompt.append("- 未能自动识别相关表，请根据Schema自行选择\n");
        }
        prompt.append("\n");

        // 表关系
        if (tableSelection != null && tableSelection.getRelations() != null
                && !tableSelection.getRelations().isEmpty()) {
            prompt.append("【表关联关系】\n");
            for (TableSelection.TableRelation relation : tableSelection.getRelations()) {
                prompt.append("- ").append(relation.getLeftTable())
                        .append(".").append(relation.getLeftColumn())
                        .append(" = ").append(relation.getRightTable())
                        .append(".").append(relation.getRightColumn())
                        .append(" [").append(relation.getJoinType()).append("]\n");
            }
            prompt.append("\n");
        }

        // 上下文信息
        if (context != null && context.canContinueFromContext()) {
            prompt.append("【上下文信息】\n");
            prompt.append(context.getContextDescription()).append("\n");
        }

        // 数据库Schema
        prompt.append("【数据库Schema】\n");
        prompt.append("```json\n").append(schemaInfo).append("\n```\n\n");

        // 用户需求
        prompt.append("【用户需求】\n");
        prompt.append(userQuery).append("\n\n");

        // 生成要求
        prompt.append("【生成要求】\n");
        prompt.append("1. 只返回SQL语句，不要任何解释\n");
        prompt.append("2. 使用").append(currentDialect.getDisplayName()).append("方言语法\n");
        prompt.append("3. 确保SQL语法完全正确\n");
        prompt.append("4. 如果需要JOIN，确保ON条件正确\n");
        prompt.append("5. 使用").append(dialectAdapter.getConfig(currentDialect).getIdentifierQuote())
                .append("表名/列名").append(dialectAdapter.getConfig(currentDialect).getIdentifierQuote()).append("包裹标识符\n");

        // 如果是模糊意图，明确指示使用默认配置
        if (isAmbiguous) {
            prompt.append("6. ⚠️ 当前查询意图不明确，必须包含以下配置：\n");
            if (effectiveSort != null && effectiveSort.getOrderByFields() != null && !effectiveSort.getOrderByFields().isEmpty()) {
                prompt.append("   - 必须按").append(String.join(", ", effectiveSort.getOrderByFields()))
                        .append(effectiveSort.isDescending() ? "降序" : "升序").append("排序\n");
            }
            if (effectivePagination != null && effectivePagination.getLimit() > 0) {
                prompt.append("   - 必须限制返回").append(effectivePagination.getLimit()).append("条记录\n");
            }
        }

        prompt.append("\nSQL：");

        return prompt.toString();
    }

    /**
     * 格式化歧义响应
     */
    private String formatAmbiguousResponse(List<AmbiguityResolver.AmbiguityInterpretation> interpretations) {
        StringBuilder sb = new StringBuilder();

        sb.append("【⚠️ 识别到模糊意图】\n");
        sb.append("您的查询可能存在多种理解方式，请提供更多信息：\n\n");

        for (int i = 0; i < interpretations.size(); i++) {
            AmbiguityResolver.AmbiguityInterpretation interp = interpretations.get(i);
            sb.append("  ").append(i + 1).append(". ").append(interp.getReason()).append("\n");
            sb.append("     可能性：").append(String.format("%.0f%%", interp.getProbability() * 100)).append("\n");
            if (interp.getAssumptions() != null && !interp.getAssumptions().isEmpty()) {
                sb.append("     假设：").append(String.join(", ", interp.getAssumptions())).append("\n");
            }
        }

        // 汇总澄清问题
        Set<String> uniqueQuestions = interpretations.stream()
                .filter(i -> i.getClarificationQuestions() != null)
                .flatMap(i -> i.getClarificationQuestions().stream())
                .collect(Collectors.toSet());

        if (!uniqueQuestions.isEmpty()) {
            sb.append("\n【💡 请澄清】\n");
            uniqueQuestions.forEach(q -> sb.append("  • ").append(q).append("\n"));
        }

        sb.append("\n【📝 示例回答】\n");
        sb.append("  - \"前10条\" / \"显示前20条\"\n");
        sb.append("  - \"按销量排序\" / \"按创建时间降序\"\n");
        sb.append("  - \"最近一个月的数据\"\n");

        return sb.toString();
    }

    /**
     * 格式化正常响应
     */
    private String formatResponse(String sql, SqlValidationResult validation) {
        return formatResponse(sql, validation, null);
    }

    /**
     * 格式化正常响应（带上下文）
     */
    private String formatResponse(String sql, SqlValidationResult validation, ConversationContext context) {
        StringBuilder sb = new StringBuilder();

        // SQL结果
        sb.append("【✅ 生成的SQL】\n");
        sb.append("```sql\n").append(sql).append("\n```\n\n");

        // 方言信息
        sb.append("【📊 方言】").append(currentDialect.getDisplayName()).append("\n\n");

        // 自动填充提示（如果有）
        if (context != null && context.isAutoFilledDefaults() && context.getAutoFilledConfig() != null) {
            sb.append("【⚠️ 自动填充】\n");
            Map<String, Object> config = context.getAutoFilledConfig();
            if (config.containsKey("sortField")) {
                sb.append("  • 排序字段: ").append(config.get("sortField"))
                        .append(" (").append(config.get("sortDirection")).append(")\n");
            }
            if (config.containsKey("limit")) {
                sb.append("  • 查询限制: ").append(config.get("limit")).append(" 条\n");
            }
            sb.append("\n");
        }

        // 警告
        if (validation.getWarnings() != null && !validation.getWarnings().isEmpty()) {
            sb.append("【⚠️ 警告】\n");
            validation.getWarnings().forEach(w -> sb.append("  • ").append(w).append("\n"));
            sb.append("\n");
        }

        // 建议
        if (validation.getSuggestions() != null && !validation.getSuggestions().isEmpty()) {
            sb.append("【💡 优化建议】\n");
            validation.getSuggestions().forEach(s -> sb.append("  • ").append(s).append("\n"));
        }

        return sb.toString();
    }
}
