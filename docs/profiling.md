# sql-forge-mcp Profiling 指南

本文档介绍如何对 `sql-forge-mcp` 做性能分析和瓶颈定位。

## 快速性能基线

| 指标 | mock 阈值 | 真实环境阈值 | 测量工具 |
|------|----------|------------|---------|
| mcpHealth P95 | < 50ms | < 100ms | `test-mcp-loadtest.sh` |
| jsonSelect P95 | < 50ms | < 100ms | `test-mcp-loadtest.sh` |
| 1000 次 CRUD heap 增长 | < 50MB | — | JUnit `ChaosAndPerformanceTest` |

跑基线：

```bash
# 单元测试内置基线（mock，每次 PR 都跑）
mvn -pl sql-forge-mcp test -Dtest=ChaosAndPerformanceTest

# 真实环境基线（每次发布前跑）
./sql-forge-test/scripts/test-mcp-loadtest.sh --users 50 --requests 200
```

---

## 工具 1：async-profiler（CPU + 内存火焰图）

`async-profiler` 是低开销采样 profiler，适合生产环境。

### 安装

```bash
# Linux / macOS
curl -L https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-linux-x64.tar.gz | tar xz
# Windows: 下载 https://github.com/async-profiler/async-profiler/releases
```

### 用法

```bash
# CPU 火焰图（60 秒采样）
java -jar async-profiler/bin/asprof.jar -e cpu -d 60 -f /tmp/mcp-cpu.html \
    -jar sql-forge-mcp/target/sql-forge-mcp-1.0-SNAPSHOT.jar \
    --sql.forge.mcp.systems[0].name=TestSys \
    --sql.forge.mcp.systems[0].url=http://localhost:8081 \
    ...

# 内存分配火焰图
java -jar async-profiler/bin/asprof.jar -e alloc -d 60 -f /tmp/mcp-alloc.html -jar ...

# 锁竞争
java -jar async-profiler/bin/asprof.jar -e lock -d 60 -f /tmp/mcp-lock.html -jar ...
```

### 火焰图怎么看

打开 `/tmp/mcp-cpu.html`：
- **Y 轴** = 调用栈深度（顶层 = 入口，越往下 = 越深）
- **X 轴** = CPU 时间占比（越宽 = 占 CPU 越多）
- **颜色** = 同一调用栈（相同颜色 = 同一父函数）

**找瓶颈**：找最宽的方块 → 看它的栈 → 找到占用最多的子调用。

---

## 工具 2：JDK Flight Recorder (JFR)

JDK 内置，零配置开销，适合长期 profiling。

### 用法

```bash
# 启动 MCP server 时开启 JFR
java -XX:StartFlightRecording=duration=60s,filename=/tmp/mcp.jfr,settings=profile \
    -jar sql-forge-mcp-1.0-SNAPSHOT.jar ...

# 或运行时启动（PID 已知）
jcmd <PID> JFR.start duration=60s filename=/tmp/mcp.jfr
jcmd <PID> JFR.dump filename=/tmp/mcp.jfr
```

### 分析工具

- **JDK Mission Control** (JMC) —— 官方 GUI，免费
- **JMC Analyzer**：火焰图、内存泄漏、锁竞争一应俱全

---

## 工具 3：JVM 指标（最轻量）

```bash
# 实时 GC / heap / 线程
jcmd <PID> GC.heap_info
jcmd <PID> Thread.print
jcmd <PID> VM.system_properties

# 内存采样（每 1 秒一次）
jcmd <PID> PerfCounter.print

# 类加载情况
jcmd <PID> VM.classloader_stats
```

---

## sql-forge-mcp 常见瓶颈清单

### 瓶颈 1：MCP stdio JSON 解析（已修复）

**症状**：Tool 调用后挂死 / hang
**根因**：MCP SDK 0.18.x 用 `new InputStreamReader(inputStream)`（无 charset）读 stdin；Windows 默认 GBK 导致 UTF-8 中文 3 字节被解码成 1-2 chars → Content-Length 错位
**修复**：`.mcp.json` 加 `--java-options=-Dfile.encoding=UTF-8`
**复现**：跑大量含中文参数的 Tool 序列

### 瓶颈 2：RestClient 连接池未配置

**症状**：高并发下连接耗尽，p99 飙升
**根因**：`SimpleClientHttpRequestFactory` 默认无连接池（每次新建 socket）
**修复**：

```java
// 在 SqlForgeMcpApplication.restClient() 中替换为：
HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
factory.setConnectTimeout(3000);
factory.setReadTimeout(10000);
return RestClient.builder().requestFactory(factory).build();
```

（需要 `httpclient5` 依赖）

### 瓶颈 3：Playwright Chromium 冷启动

**症状**：首次 `previewAmisTemplate` 慢 5-10s
**根因**：Playwright 首次启动 Chromium 子进程（懒加载）
**优化**：
- 接受现状（首次慢，后续快）
- 或在 MCP server 启动时预热（启动时调一次 `previewAmisTemplate`）

### 瓶颈 4：JSON CRUD 序列化开销

**症状**：`jsonSelect` 大表返回慢
**根因**：`RestClient` 默认用 Jackson 序列化整个响应（无 streaming）
**优化**：
- 后端支持分页 / 限制返回行数
- 前端用 `WebClient` 替换 `RestClient`（响应式 streaming）

### 瓶颈 5：日志写入阻塞

**症状**：高并发时 Tool 调用延迟增加
**根因**：Logback 同步 appender（FILE appender）阻塞 IO
**优化**：
- 用 `LogstashAsyncAppender` 或 `AsyncAppender`
- 或降级日志级别到 WARN

---

## sql-forge-mcp 内存模型

| 组件 | 占用 | 备注 |
|------|------|------|
| JVM 基础 | ~50MB | Spring Boot 启动后 |
| MCP SDK | ~30MB | spring-ai + mcp-core |
| Playwright Chromium | ~150MB | 子进程，独立 |
| Playwright Node driver | ~50MB | 子进程，独立 |
| 业务缓存（54 组件 + 17 范例） | ~5MB | AmisKnowledgeService 一次性加载 |
| **MCP 主进程总计** | **~100MB** | 不含 Playwright |

---

## 性能调优清单（推荐顺序）

1. **JVM 参数**：`-Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=50`
2. **日志**：控制台 → stderr；FILE → 异步 appender
3. **RestClient**：切到 `HttpComponentsClientHttpRequestFactory` + 连接池
4. **Tool 级超时**：CRUD 5s、渲染 30s、元数据 3s
5. **Playwright**：禁用（用 `optional=true`），只给需要渲染的环境装
6. **响应缓存**：`amis://schema-hints` 等不变 Resource 加 HTTP 缓存（30 天）

---

## 相关测试

| 测试 | 文件 | 用途 |
|------|------|------|
| `ChaosAndPerformanceTest` | `src/test/java/.../ChaosAndPerformanceTest.java` | mock P95 门禁 |
| `test-mcp-loadtest.sh` | `sql-forge-test/scripts/` | 真实环境压测 |
| `test-mcp-chaos.sh` | `sql-forge-test/scripts/` | 真实环境混沌 |

---

## 排障流程

```
Tool 调用慢 / 挂死
├─ 是不是字符集问题？→ 看 mcp-server.log 是否有 JsonParseException
├─ 是不是后端慢？→ mcpHealth 返回 UP 但 getMetaDataTables 超时
│   └─ 看 sql-forge-test 日志，找慢 SQL
├─ 是不是 MCP 进程慢？→ 跨进程排查，async-profiler 抓火焰图
├─ 是不是 Playwright 慢？→ previewAmisTemplate 单独慢
│   └─ 检查 Chromium 是否在跑（tasklist | grep chrome）
└─ 是不是资源耗尽？→ jcmd <PID> Thread.print 看线程数
```

---

## 参考

- async-profiler: https://github.com/async-profiler/async-profiler
- JDK Mission Control: https://jdk.java.io/jmc/
- JFR 文档: https://docs.oracle.com/en/java/javase/17/jfapi/
- sql-forge-mcp 性能优化建议: 见 CLAUDE.md「Key Configuration」节
