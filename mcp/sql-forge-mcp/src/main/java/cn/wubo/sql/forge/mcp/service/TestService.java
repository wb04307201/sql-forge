package cn.wubo.sql.forge.mcp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestService {

    private final RestClient restClient;
    private final ChatModel chatModel;

    /**
     * 将自然语言转换为SQL（标准入口）
     *
     * @param content 自然语言查询描述
     *
     * @return 生成的SQL语句
     */
    @Tool(name = "simple", description = "将自然语言描述的需求转换成SQL查询语句")
    public String simple(@ToolParam(description = "自然语言描述的需求，例如：查询所有年龄大于30岁的用户，按姓名升序排列") String content) throws JsonProcessingException {

        PromptTemplate promptTemplate = PromptTemplate.builder()
                .renderer(StTemplateRenderer.builder().startDelimiterToken('{').endDelimiterToken('}').build())
                .template("""
                        根据 METADATA 部分提供的数据库定义，编写一个 SQL 查询来回答 QUESTION 部分的问题。
                        仅生成 SELECT 查询语句。如果问题会导致 INSERT、UPDATE 或 DELETE 操作，
                        或者查询会以任何方式修改 DDL，请说明该操作不被支持。
                        如果问题无法回答，请说明 DDL 不支持回答该问题。
                        
                        仅回答原始 SQL 查询；不要包含 markdown 或其他不属于查询本身的标点符号。
                        
                        
                        QUESTION
                        {question}
                        
                        METADATA
                        ```json
                        {metaData}
                        ```
                        """)
                .build();

        String metaDataJson = getMetaDataFromApi();

        String prompt = promptTemplate.render(Map.of("question", content,"metaData", metaDataJson));

        return ChatClient.builder(chatModel).build().prompt().user(prompt).call().content();
    }


        /**
         * 从API获取元数据信息
         */
        private String getMetaDataFromApi() {
            try {
                return restClient.get()
                        .uri("/sql/forge/api/database/metaDataTables")
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
}
