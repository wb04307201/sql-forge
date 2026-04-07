package cn.wubo.sql.forge.mcp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author wangyong
 * @version 1.0
 * @description GenerateJsonService
 * @date 2026-04-01 19:42:40
 */
@Slf4j
@Service
public class GenerateJsonService {

    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private ChatClient chatClient;
    @Autowired
    private RestClient restClient;
    @Value("${sql-forge.api.url:http://localhost:8081}")
    private String apiBaseUrl;
    /**
     * 提示词模板
     */
    public static final String PROMPT_FILE = "classpath:doc/prompt.md";
    /**
     * 示例表信息文件
     */
    public static final String EXAMPLE_TABLE_INFO_FILE = "classpath:doc/example_table_info.md";

    /**
     * 示例Amis信息文件
     */
    public static final String EXAMPLE_AMIS_INFO_FILE = "classpath:doc/example_amis_info.md";
    /**
     * API规范文件
     */
    public static final String API_SPEC_FILE = "classpath:doc/api_spec.md";

    /**
     * 表信息示例占位符
     */
    public static final String EXAMPLE_TABLE_INFO_PLACEHOLDER = "{{EXAMPLE_TABLE_INFO}}";
    /**
     * AMIS信息示例占位符
     */
    public static final String EXAMPLE_AMIS_INFO_PLACEHOLDER = "{{EXAMPLE_AMIS_INFO}}";
    /**
     * API规范占位符
     */
    public static final String API_SPEC_PLACEHOLDER = "{{API_SPEC}}";
    /**
     * 表信息占位符
     */
    public static final String TABLE_INFO_PLACEHOLDER = "{{TABLE_INFO}}";

    /**
     * 将自然语言转换为SQL
     */
    @Tool(name = "GenerateJson", description = "根据建表语句生成符合Amis规范的单表维护界面JSON配置")
    public String generateJson(@ToolParam(description = "建表语句") String tableInfo) {
        log.info("开始生成JSON配置，表信息：{}", tableInfo);
        try {
            String prompt = readFile(PROMPT_FILE);
            String exampleTableInfo = readFile(EXAMPLE_TABLE_INFO_FILE);
            String exampleAmisInfo = readFile(EXAMPLE_AMIS_INFO_FILE);
            String apiSpec = readFile(API_SPEC_FILE);

            prompt = prompt.replace(EXAMPLE_TABLE_INFO_PLACEHOLDER, exampleTableInfo);
            prompt = prompt.replace(EXAMPLE_AMIS_INFO_PLACEHOLDER, exampleAmisInfo);
            prompt = prompt.replace(API_SPEC_PLACEHOLDER, apiSpec);
            prompt = prompt.replace(TABLE_INFO_PLACEHOLDER, tableInfo);

            log.info("==============生成模板的提示词: {}", prompt);
            ChatClient.CallResponseSpec call = chatClient.prompt()
                    .system("根据用户需求只返回json,不要返回任何多余内容。")
                    .user(prompt)
                    .call();
            String content = call.content();
            log.info("===============大模型返回的json内容: {}", content);
            return content;

        } catch (IOException e) {
            log.error("生成json失败", e);
            return "生成json失败";
        }
    }

    @Tool(name = "GenerateJsonSave", description = "保存JSON配置模板")
    public String GenerateJsonSave(@ToolParam(description = "JSON配置模板") String context) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            String id = UUID.randomUUID().toString();
            requestBody.put("id", id);
            requestBody.put("context", context);


            String result = restClient.put()
                    .uri("/sql/forge/api/template/amis")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return apiBaseUrl + "/sql/forge/console?id=" + id;

        } catch (HttpClientErrorException e) {
            String errorMsg = "保存模版失败，状态码: " + e.getStatusCode() + "，响应: " + e.getResponseBodyAsString();
            return errorMsg;
        } catch (Exception e) {
            String errorMsg = "保存模版失败: " + e.getMessage();
            return errorMsg;
        }
    }

    private String readFile(String filePath) throws IOException {
        Resource resource = resourceLoader.getResource(filePath);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

}
