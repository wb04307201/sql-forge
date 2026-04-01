package cn.wubo.sql.forge.mcp;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class NL2SQLService {

    private final ChatClient chatClient;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    // API配置
    private final String apiBaseUrl;
    private final String executorName;

    @Autowired
    public NL2SQLService(
            @Lazy ChatClient.Builder chatClientBuilder,
            @Value("${sql-forge.api.url:http://localhost:8081}") String apiBaseUrl,
            @Value("${sql-forge.api.executor:database}") String executorName) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = new ObjectMapper();
        this.apiBaseUrl = apiBaseUrl;
        this.executorName = executorName;

        // 构建 RestClient（移除重复的错误处理器）
        this.restClient = RestClient.builder()
                .baseUrl(apiBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    /**
     * 将自然语言转换为SQL
     */
    @Tool(name = "NL2SQL", description = "将自然语言描述的需求转换成SQL查询语句")
    public String NL2SQL(@ToolParam(description = "自然语言描述的需求，例如：查询所有年龄大于30岁的用户") String content) {
        try {
            // 1. 调用API获取元数据
            String schemaInfo = getMetaDataFromApi();

            // 2. 构建提示词
            String prompt = buildPrompt(content, schemaInfo);

            // 3. 调用大模型生成SQL
            String sql = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            return sql;
        } catch (Exception e) {
            return "生成SQL失败: " + e.getMessage();
        }
    }

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
            return "获取元数据失败，状态码: " + e.getStatusCode() + ", 响应: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "调用API失败: " + e.getMessage();
        }
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(String userQuery, String schemaInfo) {
        return String.format("""
                你是一个专业的SQL生成助手。根据提供的数据库Schema信息，将用户的自然语言查询转换为标准的SQL语句。
                
                数据库Schema信息：
                ```json
                %s
                ```
                
                用户需求：%s
                
                要求：
                1. 仅返回SQL语句，不要任何解释
                2. 使用标准SQL语法
                3. 如果查询涉及多个表，请使用适当的JOIN
                4. 如果需求不明确，请返回最合理的SQL
                5. 不要添加任何Markdown格式
                
                SQL语句：
                """, schemaInfo, userQuery);
    }

    /**
     * 执行SQL并返回结果
     */
    @Tool(name = "ExecuteSQL", description = "执行SQL查询并返回结果")
    public String executeSQL(@ToolParam(description = "要执行的SQL语句") String sql) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sql", sql);
//            requestBody.put("params", null);

            String result = restClient.post()
                    .uri("/sql/forge/api/database/execute?executorName={executor}", executorName)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return result;
        } catch (HttpClientErrorException e) {
            return "执行SQL失败，状态码: " + e.getStatusCode() + "，响应: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "执行SQL失败: " + e.getMessage();
        }
    }
}
