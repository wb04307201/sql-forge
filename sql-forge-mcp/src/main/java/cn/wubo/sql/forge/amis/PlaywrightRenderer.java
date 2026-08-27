package cn.wubo.sql.forge.amis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 基于 headless Chromium 的 Amis 模板真实渲染器。
 * <p>
 * 本类为 sql-forge-mcp 模块自包含实现，<b>不持有任何 web 入口</b>（servlet / RouterFunction），
 * 也<b>不依赖</b>{@code sql-forge-web} 模块。所有 HTML 在 Java 进程内拼装后通过
 * {@link Page#setContent(String)} 注入到 {@code about:blank}，不发起任何 HTTP 请求加载页面。
 * </p>
 * <p>
 * Amis SDK（JS / CSS）由浏览器按需从 jsdelivr CDN 拉取，版本号由
 * {@link #AMIS_SDK_VERSION} 控制。CDN 不可达时静态校验结果不受影响，
 * 渲染会标记为 {@code rendered=false} 并记录 network 错误。
 * </p>
 * <p>
 * fetcher 用 XMLHttpRequest 而非 fetch：避免 about:blank 跨域到业务后端时触发 preflight。
 * </p>
 */
public class PlaywrightRenderer {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightRenderer.class);

    /**
     * Amis SDK 锁版本号，避免大版本升级带来兼容问题。
     */
    public static final String AMIS_SDK_VERSION = "6.12.0";

    /**
     * Amis SDK CSS CDN 地址。
     * <p>
     * amis 6.x 包结构：
     * <ul>
     *   <li>{@code /lib/index.min.js} — 包主入口（CJS，浏览器里不能直接用，会报 {@code exports is not defined}）</li>
     *   <li>{@code /sdk/sdk.js} — UMD 浏览器可用 bundle（含全部 SDK + 主题 CSS）</li>
     *   <li>{@code /sdk/sdk.css} — SDK 全量 CSS（含 cxd/antd/dark 三主题）</li>
     * </ul>
     * 历史错误：用过 {@code sdk.js}（404）、{@code lib/index.min.js}（CJS），这次迭代测出来改正。
     */
    public static final String AMIS_SDK_CSS = "https://cdn.jsdelivr.net/npm/amis@" + AMIS_SDK_VERSION + "/sdk/sdk.css";

    /**
     * Amis SDK JS CDN 地址（UMD bundle，浏览器直接可用）。
     */
    public static final String AMIS_SDK_JS = "https://cdn.jsdelivr.net/npm/amis@" + AMIS_SDK_VERSION + "/sdk/sdk.js";

    /**
     * 等待首次渲染完成的超时（毫秒）。
     */
    public static final int RENDER_WAIT_MS = 3000;

    /**
     * Chromium 启动超时（毫秒）：超过这个时间认为启动失败，降级返回。
     * 避免 Playwright 在没有预装浏览器时尝试下载而长时间阻塞。
     */
    public static final int LAUNCH_TIMEOUT_MS = 10_000;

    /**
     * 进程内复用的浏览器实例；未启动时为 {@code null}。
     */
    private volatile BrowserHolder browserHolder;

    /**
     * 应用关闭时释放 Chromium 进程。
     */
    @PreDestroy
    public void shutdown() {
        BrowserHolder holder = this.browserHolder;
        if (holder != null) {
            try {
                holder.close();
            } catch (Exception ex) {
                log.warn("Playwright 浏览器关闭失败: {}", ex.getMessage());
            }
            this.browserHolder = null;
        }
    }

    /**
     * 渲染一个 Amis 模板并收集渲染过程中的错误。
     * <p>
     * 若 Chromium 不可用（未安装 / 启动失败），返回 {@link PreviewResult}，
     * 其 {@code render.available=false}、{@code render.installHint} 含安装命令。
     * </p>
     *
     * @param systemName 业务后端系统名（fetcher 访问对应 baseUrl，可为 null/空 表示不调用业务 API）
     * @param context    模板 JSON 字符串
     * @return 渲染结果（静态 + 渲染信息）
     */
    public PreviewResult render(String systemName, String context) {
        return render(systemName, null, context);
    }

    /**
     * 带 baseUrl 的渲染版本：把后端 baseUrl 注入 HTML,让 makeFetcher 把相对 URL(amis
     * CRUD 模板最常见的 "POST /sql/forge/..." 写法)拼成绝对 URL,避免 about:blank 上
     * XHR.open 抛 "Invalid URL"。可空(空 = 不解析相对 URL,行为同旧版)。
     */
    public PreviewResult render(String systemName, String baseUrl, String context) {
        BrowserHolder holder;
        try {
            holder = acquireBrowser();
        } catch (Exception ex) {
            log.warn("Chromium 启动失败: {}", ex.getMessage());
            return unavailable(ex.getMessage());
        }
        String html = buildHtml(context, baseUrl);
        try (BrowserContext ctx = holder.browser.newContext();
             Page page = ctx.newPage()) {
            List<PreviewError> errors = new ArrayList<>();
            // 捕获未捕获异常（schema 错误导致 amis 渲染时崩溃）
            page.onPageError(message -> errors.add(new PreviewError("pageerror", message, null)));
            // 捕获 amis / fetcher 自记录的 console.error
            page.onConsoleMessage(msg -> {
                if ("error".equalsIgnoreCase(msg.type())) {
                    errors.add(new PreviewError("console", msg.text(), null));
                }
            });
            // 捕获业务 api 网络失败（仅过滤掉 SDK 静态资源）
            page.onRequestFailed(req -> {
                String url = req.url();
                if (url.contains("/sql/forge/api/") || url.contains("localhost:8081")) {
                    String failure = req.failure();
                    errors.add(new PreviewError("network", "API 请求失败: " + url + " (" + failure + ")", url));
                }
            });
            page.setContent(html, new Page.SetContentOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            // 等 SDK 拉取 + 首次渲染
            page.waitForTimeout(RENDER_WAIT_MS);
            return new PreviewResult(true, true, null, errors);
        } catch (Exception ex) {
            log.warn("渲染失败: {}", ex.getMessage());
            return new PreviewResult(true, false, ex.getMessage(), List.of(new PreviewError("render", ex.getMessage(), null)));
        }
    }

    /**
     * 渲染结果（与 MCP @Tool 返回结构一致）。
     *
     * @param available 渲染器是否可用（Chromium 能否启动）
     * @param rendered  本次是否成功渲染
     * @param reason    失败原因（成功时为 null）
     * @param errors    错误列表（pageerror / console / network / render）
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PreviewResult(boolean available, boolean rendered, String reason, List<PreviewError> errors) {
    }

    /**
     * 单条渲染错误。
     *
     * @param source  错误来源：pageerror / console / network / render
     * @param message 错误描述
     * @param url     相关 URL（network 类错误时填）
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PreviewError(String source, String message, String url) {
    }

    /**
     * 返回渲染不可用的降级结果（Chromium 未安装 / 启动失败）。
     *
     * @param reason 失败原因
     * @return 仅含 installHint 的渲染结果
     */
    public PreviewResult unavailable(String reason) {
        return new PreviewResult(false, false, reason, List.of());
    }

    /**
     * 拼装自包含 HTML：不向任何业务后端发请求加载页面，CDN 由浏览器按需拉取。
     *
     * @param context Amis 模板 JSON 字符串（将 base64 嵌入）
     * @return 完整 HTML 字符串
     */
    String buildHtml(String context) {
        return buildHtml(context, null);
    }

    /**
     * 带 baseUrl 的 HTML 拼装:把 baseUrl 写入 window.__AMIS_BASE_URL__,让 makeFetcher
     * 把相对 API URL 拼成绝对 URL。可空(空 = 旧行为,相对 URL 直接传给 XHR)。
     */
    String buildHtml(String context, String baseUrl) {
        String b64 = Base64.getEncoder().encodeToString((context == null ? "" : context).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        // baseUrl 转义:防止 HTML/JS 注入(虽然 config 来自 .mcp.json 但仍防御)
        String baseUrlJson;
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrlJson = "null";
        } else {
            // JSON.stringify 自动转义,后端用 JSON.parse 取回
            baseUrlJson = "\"" + baseUrl.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        // amis 6.x SDK 的完整资源（jsdelivr CDN）：
        //   - sdk.js (UMD bundle, 通过 amisRequire 暴露 amis/embed 模块)
        //   - sdk.css + helper.css + iconfont.css + cxd.css (主题)
        //   - React + ReactDOM (amis 6.x 需要 React 16.8+，默认用 React 17 UMD)
        // 同时加载顺序必须是 React/ReactDOM 先于 amis SDK，否则 amis 内部的 React 引用为 null。
        // language=HTML
        return "<!doctype html>\n"
                + "<html lang=\"zh-CN\">\n"
                + "<head>\n"
                + "<meta charset=\"utf-8\">\n"
                + "<meta http-equiv=\"Content-Security-Policy\""
                + " content=\"default-src 'self' https: cdn.jsdelivr.net;"
                + " script-src 'unsafe-inline' 'unsafe-eval' https: cdn.jsdelivr.net;"
                + " style-src 'unsafe-inline' https: cdn.jsdelivr.net;"
                + " img-src 'self' data: https: cdn.jsdelivr.net;"
                + " font-src 'self' data: https: cdn.jsdelivr.net;"
                + " object-src 'none';"
                + " base-uri 'none';"
                + " connect-src 'self' http://localhost:* http://127.0.0.1:* https:;\">\n"
                + "<title>Amis Preview</title>\n"
                + "<link rel=\"stylesheet\" href=\"" + AMIS_SDK_CSS + "\">\n"
                + "<link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/amis@" + AMIS_SDK_VERSION + "/sdk/helper.css\">\n"
                + "<link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/amis@" + AMIS_SDK_VERSION + "/sdk/iconfont.css\">\n"
                + "<link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/amis@" + AMIS_SDK_VERSION + "/sdk/cxd.css\">\n"
                + "</head>\n"
                + "<body>\n"
                + "<div id=\"root\"></div>\n"
                + "<script crossorigin src=\"https://cdn.jsdelivr.net/npm/react@17.0.2/umd/react.production.min.js\"></script>\n"
                + "<script crossorigin src=\"https://cdn.jsdelivr.net/npm/react-dom@17.0.2/umd/react-dom.production.min.js\"></script>\n"
                + "<script>\n"
                + "(function(){\n"
                + "  // about:blank 加载下 window.localStorage / document.cookie / history.replaceState 都会抛 SecurityError。\n"
                + "  // 这些都是 amis 6.x 启动时必经的调用。在 SDK 加载前先 shim，让业务逻辑正常执行。\n"
                + "  // localStorage shim\n"
                + "  try { window.localStorage.getItem('__probe__'); } catch(e) {\n"
                + "    var _ls = {};\n"
                + "    var lsShim = {\n"
                + "      getItem:function(k){return Object.prototype.hasOwnProperty.call(_ls, k) ? _ls[k] : null;},\n"
                + "      setItem:function(k,v){_ls[k]=String(v);},\n"
                + "      removeItem:function(k){delete _ls[k];},\n"
                + "      clear:function(){_ls={};},\n"
                + "      key:function(i){return Object.keys(_ls)[i] || null;},\n"
                + "      get length(){return Object.keys(_ls).length;}\n"
                + "    };\n"
                + "    try { Object.defineProperty(window, 'localStorage', {value: lsShim, configurable: true}); } catch(ex) { window.localStorage = lsShim; }\n"
                + "    try { Object.defineProperty(window, 'sessionStorage', {value: lsShim, configurable: true}); } catch(ex) {}\n"
                + "  }\n"
                + "  // document.cookie shim（about:blank 不能读写 cookie）\n"
                + "  try { document.cookie = 'p=1'; document.cookie = 'p=; expires=Thu, 01 Jan 1970 00:00:00 GMT'; }\n"
                + "  catch(e) {\n"
                + "    try { Object.defineProperty(document, 'cookie', {get:function(){return '';}, set:function(){}, configurable:true}); } catch(ex) {}\n"
                + "  }\n"
                + "  // history.replaceState shim（about:blank origin 'null' 不允许带 URL 的 replaceState）\n"
                + "  if (window.history && typeof window.history.replaceState === 'function') {\n"
                + "    var _origReplace = window.history.replaceState.bind(window.history);\n"
                + "    var _origPush = window.history.pushState.bind(window.history);\n"
                + "    window.history.replaceState = function(state, title, url){\n"
                + "      try { return _origReplace(state, title, url || location.href); }\n"
                + "      catch(ex) { /* silently ignore */ }\n"
                + "    };\n"
                + "    window.history.pushState = function(state, title, url){\n"
                + "      try { return _origPush(state, title, url || location.href); }\n"
                + "      catch(ex) { /* silently ignore */ }\n"
                + "    };\n"
                + "  }\n"
                + "  window.__amisEnvShimmed = true;\n"
                + "})();\n"
                + "</script>\n"
                + "<script src=\"" + AMIS_SDK_JS + "\"></script>\n"
                + "<script>\n"
                + "(function(){\n"
                + "  var b64 = '" + b64 + "';\n"
                + "  function b64Decode(str){try{return decodeURIComponent(atob(str).split('').map(function(c){return '%'+('00'+c.charCodeAt(0).toString(16)).slice(-2);}).join(''));}catch(e){return atob(str);}}\n"
                + "  var schema;\n"
                + "  try { schema = JSON.parse(b64Decode(b64)); } catch(e) { document.body.innerText='JSON parse error: '+e.message; return; }\n"
                + "  // 业务后端 baseUrl(可空)。makeFetcher 用它把相对 URL 拼成绝对 URL\n"
                + "  // —— 修 about:blank 上 XHR.open('POST /sql/forge/...') 抛 'Invalid URL' 的问题\n"
                + "  window.__AMIS_BASE_URL__ = " + baseUrlJson + ";\n"
                + "  function makeFetcher(){\n"
                + "    return function(req){\n"
                + "      return new Promise(function(resolve){\n"
                + "        try {\n"
                + "          // 相对 URL 解析:amis CRUD 模板常见 'POST /sql/forge/...' 写法\n"
                + "          var url = req.url;\n"
                + "          if (window.__AMIS_BASE_URL__ && url && url.charAt(0) === '/' && !/^https?:/i.test(url)) {\n"
                + "            try { url = new URL(url, window.__AMIS_BASE_URL__).href; } catch(e) { /* keep raw */ }\n"
                + "          }\n"
                + "          var xhr = new XMLHttpRequest();\n"
                + "          xhr.open(req.method || 'GET', url, true);\n"
                + "          if (req.headers) { for (var k in req.headers) { if (req.headers[k] != null) xhr.setRequestHeader(k, req.headers[k]); } }\n"
                + "          xhr.responseType = 'text';\n"
                + "          xhr.onload = function(){\n"
                + "            var body = xhr.responseText;\n"
                + "            var data = null;\n"
                + "            try { data = body ? JSON.parse(body) : null; } catch(e) { data = body; }\n"
                + "            resolve({status: xhr.status, headers: xhr.getAllResponseHeaders ? xhr.getAllResponseHeaders() : '', body: body, data: data});\n"
                + "          };\n"
                + "          xhr.onerror = function(){ console.error('XHR error', req.url); resolve({status:0, headers:'', body:'', data:null}); };\n"
                + "          var m = (req.method || 'GET').toUpperCase();\n"
                + "          if (m === 'GET' || m === 'HEAD' || m === 'DELETE') {\n"
                + "            if (req.data) {\n"
                + "              var qs = [];\n"
                + "              for (var k2 in req.data) { if (req.data[k2] != null) qs.push(encodeURIComponent(k2)+'='+encodeURIComponent(typeof req.data[k2]==='object'?JSON.stringify(req.data[k2]):req.data[k2])); }\n"
                + "              req.url += (req.url.indexOf('?')>=0?'&':'?') + qs.join('&');\n"
                + "              xhr.open(req.method || 'GET', req.url, true);\n"
                + "            }\n"
                + "            xhr.send();\n"
                + "          } else {\n"
                + "            xhr.setRequestHeader('Content-Type', 'application/json');\n"
                + "            xhr.send(req.data ? JSON.stringify(req.data) : '');\n"
                + "          }\n"
                + "        } catch(ex) {\n"
                + "          console.error('fetcher exception', ex && ex.message);\n"
                + "          resolve({status:0, headers:'', body:'', data:null});\n"
                + "        }\n"
                + "      });\n"
                + "    };\n"
                + "  }\n"
                + "  // amis 6.x: 必须通过 amisRequire('amis/embed') 取 embed 模块（window.amis 是 AMD loader，无 embed 方法）\n"
                + "  if (typeof window.amisRequire !== 'function') {\n"
                + "    document.body.innerText='Amis SDK 未加载，请检查 CDN 连通性';\n"
                + "    return;\n"
                + "  }\n"
                + "  var amisEmbed;\n"
                + "  try { amisEmbed = window.amisRequire('amis/embed'); } catch(e) {\n"
                + "    document.body.innerText='amisRequire 失败: '+e.message; return;\n"
                + "  }\n"
                + "  if (!amisEmbed || typeof amisEmbed.embed !== 'function') {\n"
                + "    document.body.innerText='amis/embed 模块未找到';\n"
                + "    return;\n"
                + "  }\n"
                + "  try {\n"
                + "    amisEmbed.embed('#root', schema, {fetcher: makeFetcher(), theme:'cxd', locale:'zh-CN'});\n"
                + "  } catch(e) {\n"
                + "    document.body.innerText='amis.embed 调用失败: '+e.message;\n"
                + "  }\n"
                + "})();\n"
                + "</script>\n"
                + "</body>\n"
                + "</html>\n";
    }

    /**
     * 获取 Chromium 浏览器（懒启动）。
     * <p>
     * 启动过程在 {@link #LAUNCH_TIMEOUT_MS} 毫秒内完成；超时则视为启动失败抛 {@link PlaywrightException}。
     * 这样 Playwright 在没有预装浏览器时不会阻塞过久。
     * </p>
     *
     * @return 浏览器持有器
     * @throws Exception 启动失败或超时
     */
    private BrowserHolder acquireBrowser() throws Exception {
        BrowserHolder holder = this.browserHolder;
        if (holder != null && holder.browser.isConnected()) {
            return holder;
        }
        synchronized (this) {
            if (this.browserHolder != null && this.browserHolder.browser.isConnected()) {
                return this.browserHolder;
            }
            java.util.concurrent.FutureTask<BrowserHolder> task = new java.util.concurrent.FutureTask<>(() -> {
                Playwright pw = Playwright.create();
                Browser browser = pw.chromium().launch(
                        // 防御纵深：让 Playwright 的 Node.js driver 不打印 deprecation 警告，
                        // 避免任何输出意外回流到父进程 stdout（MCP transport 通道）。
                        new BrowserType.LaunchOptions()
                                .setHeadless(true)
                                .setEnv(java.util.Map.of("NODE_NO_WARNINGS", "1")));
                return new BrowserHolder(pw, browser);
            });
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
            try {
                executor.submit(task);
                this.browserHolder = task.get(LAUNCH_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
                return this.browserHolder;
            } catch (java.util.concurrent.TimeoutException te) {
                task.cancel(true);
                throw new RuntimeException("Chromium 启动超时（> " + LAUNCH_TIMEOUT_MS + "ms），请确认浏览器已安装", te);
            } finally {
                executor.shutdownNow();
            }
        }
    }

    /**
     * Chromium 浏览器持有器。
     *
     * @param pw      Playwright 实例
     * @param browser 浏览器实例
     */
    record BrowserHolder(Playwright pw, Browser browser) {
        void close() {
            try {
                browser.close();
            } catch (Exception ignore) {
            }
            try {
                pw.close();
            } catch (Exception ignore) {
            }
        }
    }
}