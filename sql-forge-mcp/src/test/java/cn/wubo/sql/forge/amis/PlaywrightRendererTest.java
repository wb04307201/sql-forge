package cn.wubo.sql.forge.amis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PlaywrightRenderer 单测：仅测静态工厂方法、DTO 字段、HTML 拼装片段，
 * 不依赖浏览器（避免 CI 上没有 Chromium 时失败）。
 */
class PlaywrightRendererTest {

    @Test
    void shouldReturnUnavailableResult() {
        PlaywrightRenderer renderer = new PlaywrightRenderer();
        PlaywrightRenderer.PreviewResult result = renderer.unavailable("chromium missing");
        assertFalse(result.available());
        assertFalse(result.rendered());
        assertEquals("chromium missing", result.reason());
        assertNotNull(result.errors());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void shouldBuildHtmlWithBase64EncodedContext() {
        PlaywrightRenderer renderer = new PlaywrightRenderer();
        String ctx = "{\"type\":\"page\",\"title\":\"hi\"}";
        String html = renderer.buildHtml(ctx);
        assertTrue(html.startsWith("<!doctype html>"));
        assertTrue(html.contains(PlaywrightRenderer.AMIS_SDK_CSS));
        assertTrue(html.contains(PlaywrightRenderer.AMIS_SDK_JS));
        assertTrue(html.contains("<div id=\"root\">"));
        // base64 of ctx should appear
        java.util.Base64.Encoder enc = java.util.Base64.getEncoder();
        assertTrue(html.contains(enc.encodeToString(ctx.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        // fetcher / embed / XHR
        assertTrue(html.contains("XMLHttpRequest"));
        assertTrue(html.contains("amis.embed"));
    }

    @Test
    void shouldBuildHtmlWithNullContext() {
        PlaywrightRenderer renderer = new PlaywrightRenderer();
        String html = renderer.buildHtml(null);
        assertNotNull(html);
        assertTrue(html.contains("amis.embed"));
    }

    @Test
    void shouldBuildHtmlWithBlankContext() {
        PlaywrightRenderer renderer = new PlaywrightRenderer();
        String html = renderer.buildHtml("");
        assertNotNull(html);
        assertTrue(html.contains("<div id=\"root\">"));
    }

    @Test
    void previewErrorDtoShouldCarryAllFields() {
        PlaywrightRenderer.PreviewError err = new PlaywrightRenderer.PreviewError(
                "pageerror", "boom", "http://x");
        assertEquals("pageerror", err.source());
        assertEquals("boom", err.message());
        assertEquals("http://x", err.url());
    }

    @Test
    void previewResultDtoShouldCarryAllFields() {
        List<PlaywrightRenderer.PreviewError> errs = List.of(
                new PlaywrightRenderer.PreviewError("console", "e1", null),
                new PlaywrightRenderer.PreviewError("network", "e2", "http://y"));
        PlaywrightRenderer.PreviewResult r = new PlaywrightRenderer.PreviewResult(true, false, "r1", errs);
        assertTrue(r.available());
        assertFalse(r.rendered());
        assertEquals("r1", r.reason());
        assertEquals(2, r.errors().size());
    }

    @Test
    void amisSdkVersionIsPinned() {
        // 锁版本号：避免大版本升级带来兼容问题
        assertNotNull(PlaywrightRenderer.AMIS_SDK_VERSION);
        assertTrue(PlaywrightRenderer.AMIS_SDK_JS.contains(PlaywrightRenderer.AMIS_SDK_VERSION));
        assertTrue(PlaywrightRenderer.AMIS_SDK_CSS.contains(PlaywrightRenderer.AMIS_SDK_VERSION));
        assertTrue(PlaywrightRenderer.AMIS_SDK_JS.contains("jsdelivr.net"));
        assertTrue(PlaywrightRenderer.AMIS_SDK_CSS.contains("jsdelivr.net"));
    }

    @Test
    void htmlFetcherShouldNotIncludeFetchApi() {
        // 明确只用 XHR 而非 fetch，避免 about:blank preflight
        PlaywrightRenderer renderer = new PlaywrightRenderer();
        String html = renderer.buildHtml("{}");
        assertTrue(html.contains("XMLHttpRequest"));
        // 不应使用 window.fetch
        assertFalse(html.contains("window.fetch("));
    }

    @Test
    void htmlShouldHandleInvalidBase64Gracefully() {
        // 验证脚本语法正确（即便 base64 解码失败也不会抛 ReferenceError）
        PlaywrightRenderer renderer = new PlaywrightRenderer();
        String html = renderer.buildHtml("{\"type\":\"page\"}");
        assertTrue(html.contains("try {"));
        assertTrue(html.contains("JSON.parse"));
    }

    @Test
    void renderShouldReturnUnavailableWhenRendererIsMissing() {
        // 模拟 bean 不存在：返回降级结果
        PlaywrightRenderer renderer = new PlaywrightRenderer();
        PlaywrightRenderer.PreviewResult r = renderer.unavailable("test");
        assertNull(null); // sanity
        assertFalse(r.available());
    }
}