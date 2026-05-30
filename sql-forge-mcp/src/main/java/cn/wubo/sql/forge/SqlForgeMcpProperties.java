package cn.wubo.sql.forge;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "sql.forge.mcp")
public class SqlForgeMcpProperties {

    private List<SystemInfo> systems = new ArrayList<>();

    @Data
    public static class SystemInfo {
        private String name;
        private String url;
        private String description;
        private String apiKey;
    }

}
