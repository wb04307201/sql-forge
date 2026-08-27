package cn.wubo.sql.forge;

import cn.wubo.sql.forge.amis.AmisValidator;
import cn.wubo.sql.forge.amis.PlaywrightRenderer;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Amis 校验 / 渲染：2 个 Tool。
 * <p>
 * 拆分自 {@link SqlForgeMcpService}。健康检查 Tool（mcpHealth）保留在 {@link SqlForgeMcpService}，
 * 因为需要 ping 每个后端（依赖 {@link org.springframework.web.client.RestClient}）。
 * </p>
 */
public class AmisService {

    private final McpToolSupport support;
    private final AmisValidator amisValidator;
    private final PlaywrightRenderer amisRenderer;

    public AmisService(McpToolSupport support,
                       AmisValidator amisValidator,
                       PlaywrightRenderer amisRenderer) {
        this.support = support;
        this.amisValidator = amisValidator;
        this.amisRenderer = amisRenderer;
    }

    @Tool(description = "静态校验 Amis 模板 JSON：JSON 语法、必填字段、组件 type、api 格式、嵌套递归")
    public AmisValidator.ValidationResult validateAmisTemplate(
            @ToolParam(description = "Amis 模板 JSON 字符串") String context) {
        if (amisValidator == null) {
            return new AmisValidator.ValidationResult(false,
                    java.util.List.of(new AmisValidator.ValidationError("error", "AmisValidator 不可用", "$")));
        }
        return amisValidator.validate(context);
    }

    @Tool(description = "用 headless Chromium 真实渲染 Amis 模板（自包含 HTML，不依赖外部预览页）。"
            + "Chromium 不可用时返回 render.available=false + installHint；"
            + "静态校验结果不受影响。")
    public PlaywrightRenderer.PreviewResult previewAmisTemplate(
            @ToolParam(description = "业务后端系统名（fetcher 会调用对应 baseUrl），可省略")
            String systemName,
            @ToolParam(description = "Amis 模板 JSON 字符串")
            String context) {
        if (amisRenderer == null) {
            return new PlaywrightRenderer.PreviewResult(false, false, "PlaywrightRenderer 不可用", java.util.List.of());
        }
        return amisRenderer.render(systemName, context);
    }
}
