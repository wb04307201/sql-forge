package cn.wubo.sql.forge;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "sql.forge")
public class SqlForgeProperties {

    private List<String> schemata = new ArrayList<>();
    private  CalciteProperties calcite = new CalciteProperties();
    private ApiProperties api = new ApiProperties();
    private ConsoleProperties console = new ConsoleProperties();

    @Data
    public static class CalciteProperties {
        private Boolean enabled = false;
        private String configuration;
    }

    @Data
    public static class ApiProperties {
        private DatabaseProperties database = new DatabaseProperties();
        private JsonProperties json = new JsonProperties();
        private TemplateProperties template = new TemplateProperties();

        @Data
        public static class DatabaseProperties {
            private Boolean enabled = false;
            private Boolean selectOnly = true;
        }

        @Data
        public static class JsonProperties {
            private Boolean enabled = true;
        }

        @Data
        public static class TemplateProperties {
            private SqlTemplateProperties sql = new SqlTemplateProperties();
            private AmisTemplateProperties amis = new AmisTemplateProperties();

            @Data
            public static class SqlTemplateProperties {
                private Boolean enabled = true;
            }

            @Data
            public static class AmisTemplateProperties {
                private Boolean enabled = true;
            }
        }
    }

    @Data
    public static class ConsoleProperties {
        private Boolean enabled = true;
    }
}
