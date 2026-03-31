package cn.wubo.sql.forge;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public record FileContentReader(
        ResourceLoader resourceLoader
) {

    public String readContent(String path) throws IOException {
        Resource resource = resourceLoader.getResource(path);
        return new String(
                resource.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
    }
}
