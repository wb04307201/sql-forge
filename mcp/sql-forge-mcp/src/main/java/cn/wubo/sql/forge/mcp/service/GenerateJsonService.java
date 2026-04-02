package cn.wubo.sql.forge.mcp.service;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author wangyong
 * @version 1.0
 * @description GenerateJsonService
 * @date 2026-04-01 19:42:40
 */
@Service
public class GenerateJsonService {

    @Resource
    private ChatClient chatClient;
    @Resource
    private RestClient restClient;
    @Value("${sql-forge.api.url:http://localhost:8081}")
    private  String apiBaseUrl;
    /**
     * 提示词模板
     */
    public static final String PROMPT_FILE = "src/main/resources/doc/prompt.md";
    /**
     * 示例表信息文件
     */
    public static final String EXAMPLE_TABLE_INFO_FILE = "src/main/resources/doc/example_table_info.md";

    /**
     * 示例Amis信息文件
     */
    public static final String EXAMPLE_AMIS_INFO_FILE = "src/main/resources/doc/example_amis_info.md";
    /**
     * API规范文件
     */
    public static final String API_SPEC_FILE = "src/main/resources/doc/api_spec.md";

    /**
     * 表信息示例占位符
     */
    public static final String EXAMPLE_TABLE_INFO_PLACEHOLDER = "EXAMPLE_TABLE_INFO";
    /**
     * AMIS信息示例占位符
     */
    public static final String EXAMPLE_AMIS_INFO_PLACEHOLDER = "EXAMPLE_AMIS_INFO";
    /**
     * API规范占位符
     */
    public static final String API_SPEC_PLACEHOLDER = "API_SPEC";
    /**
     * 表信息占位符
     */
    public static final String TABLE_INFO_PLACEHOLDER = "TABLE_INFO";

    /**
     * 将自然语言转换为SQL
     */
    @Tool(name = "GenerateJson", description = "根据建表语句生成符合Amis规范的单表维护界面JSON配置")
    public String generateJson(@ToolParam(description = "建表语句") String tableInfo) {
        try {
            String prompt = Files.readString(Path.of(PROMPT_FILE));
            String exampleTableInfo = Files.readString(Path.of(EXAMPLE_TABLE_INFO_FILE));
            String exampleAmisInfo = Files.readString(Path.of(EXAMPLE_AMIS_INFO_FILE));
            String apiSpec = Files.readString(Path.of(API_SPEC_FILE));

            PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("{{", "}}");
            Map<String, String> values = new HashMap<>(16);
            values.put(EXAMPLE_TABLE_INFO_PLACEHOLDER, exampleTableInfo);
            values.put(EXAMPLE_AMIS_INFO_PLACEHOLDER, exampleAmisInfo);
            values.put(API_SPEC_PLACEHOLDER, apiSpec);
            values.put(TABLE_INFO_PLACEHOLDER, tableInfo);

            String result = helper.replacePlaceholders(prompt, values::get);
            ChatClient.CallResponseSpec call = chatClient.prompt()
                    .system("根据用户需求只返回json,不要返回任何多余内容。")
                    .user(result)
                    .call();
            return call.content();

        } catch (IOException e) {
            return "生成json失败";
        }
    }

    @Tool(name = "GenerateJsonSave", description = "保存JSON配置模板")
    public String GenerateJsonSave(@ToolParam(description = "JSON配置模板") String context) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            String id = UUID.randomUUID().toString();
            requestBody.put("id",id );
            requestBody.put("context", context);
            

            String result = restClient.put()
                    .uri("/sql/forge/api/template/amis")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return apiBaseUrl+"/sql/forge/console?id=" + id;

        } catch (HttpClientErrorException e) {
            String errorMsg = "保存模版失败，状态码: " + e.getStatusCode() + "，响应: " + e.getResponseBodyAsString();
            return errorMsg;
        } catch (Exception e) {
            String errorMsg = "保存模版失败: " + e.getMessage();
            return errorMsg;
        }
    }

}
