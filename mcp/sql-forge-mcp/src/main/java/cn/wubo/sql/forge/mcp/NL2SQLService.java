package cn.wubo.sql.forge.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class NL2SQLService {

    @Tool(description = "将自然语言转换成SQL")
    public String NL2SQL(@ToolParam(description = "自然语言描述的需求") String content) {
        return "";
    }

    public static void main(String[] args) {
        NL2SQLService client = new NL2SQLService();
    }
}
