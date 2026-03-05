package cn.wubo.sql.forge;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@SpringBootTest()
@ActiveProfiles("test")
class ChatTest {

    @Autowired
    ChatClient chatClient;

    @Test
    void call() {
        String result = chatClient.prompt().user("你好!").call().content();
        log.info("result: {}", result);
    }

    @Test
    void stream() throws InterruptedException {
        Flux<String> flux = chatClient.prompt().user("你好!").stream().content();
        flux.subscribe(log::info);
        Thread.sleep(5000);
    }


    @Test
    void prompt() {
        SqlForgeProperties sqlForgeProperties = new SqlForgeProperties();
        String template = sqlForgeProperties.getAi().getPromptTemplate();
        template = template.replace("{{API_SPEC}}", sqlForgeProperties.getAi().getApiSpec());
        template = template.replace("{{TABLE_INFO}}", """
```json
[{
  "table": "PRODUCTS",
  "desc": "商品表",
  "type": "crud",
  "fields": {
    "ID": {"type": "uuid", "desc": "商品ID"},
    "NAME": {"type": "string", "length": 50, "desc": "商品名称","search": true},
    "DICT_CATEGORIES": {"type": "dict", "length": 100, "desc": "商品类型", "ref": {"type":"JOIN","table": "sys_dict_items", "on": "item_code", "filter": {"dict_code": "categories"}},"search": true},
    "PRICE": {"type": "number", "max": 9999999999, "precision": 2, "desc": "邮箱地址","search": true}
  }
},
  {
    "table": "SYS_DICT_ITEMS",
    "desc": "字典项表",
    "type": "dict",
    "fields": {
      "DICT_CODE": {"type": "string"},
      "ITEM_CODE": {"type": "string"},
      "ITEM_NAME": {"type": "string"}
    }
  }]
```
""");
        template = template.replace("{{EXAMPLE_TABLE_INFO}}", sqlForgeProperties.getAi().getExampleTableInfo());
        template = template.replace("{{EXAMPLE_AMIS_INFO}}", sqlForgeProperties.getAi().getExampleAmisInfo());


        String result = chatClient.prompt().user(template).call().content();
        log.info("result: {}", result);
    }
}
