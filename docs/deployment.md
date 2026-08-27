# sql-forge-mcp 部署 & 监控建议

本文档面向运维和 SRE：sql-forge-mcp 部署到生产环境时的检查清单、JVM 参数、端口规划、监控指标、故障排查。

## 1. 部署前置清单

### 1.1 业务后端

| 项 | 要求 |
|------|------|
| 后端服务 | `sql-forge-test` 或自研 `sql-forge-spring-boot-starter` |
| HTTP 端口 | 默认 8081（可在启动参数 `--server.port=...` 改） |
| Auth | 业务后端开启 `X-Api-Key` 鉴权（`sql.forge.apiKeys` 配置） |
| 数据库 | 任意（后端支持 H2 / MySQL / PostgreSQL） |

### 1.2 MCP server 启动前置

| 项 | 要求 |
|------|------|
| JDK | **17+**（用 `release=17` 编译，`17` 之后的新 API 禁用） |
| Maven | 3.8+（仅源码构建时需要；jar 包部署不需要） |
| Playwright | 1.50+（**可选**，缺则 `previewAmisTemplate` 返回 `available=false`） |
| httpclient5 | **可选**（缺则用 JDK `HttpURLConnection`，无连接池；推荐装上） |

### 1.3 端口 / 网络

| 端口 | 服务 | 说明 |
|------|------|------|
| 8081 | sql-forge 后端 | MCP server 调这里 |
| stdio | Claude Code → sql-forge-mcp | 进程间 JSON-RPC over stdin/stdout |

MCP server 本身**不占任何端口**（stdio transport）。

## 2. JVM 参数推荐

```bash
java \
  -Xms256m -Xmx512m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=50 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/sql-forge-mcp/ \
  -Dfile.encoding=UTF-8 \
  -Dsun.jnu.encoding=UTF-8 \
  -Dlogback.configurationFile=/etc/sql-forge-mcp/logback.xml \
  -jar sql-forge-mcp-1.0-SNAPSHOT.jar \
  --sql.forge.mcp.systems[0].name=Prod \
  --sql.forge.mcp.systems[0].url=http://sql-forge-backend:8081 \
  --sql.forge.mcp.systems[0].apiKey=${API_KEY_FROM_SECRET} \
  --spring.main.web-application-type=none
```

### 2.1 关键参数说明

| 参数 | 原因 |
|------|------|
| `-Xms256m -Xmx512m` | MCP server 自身 ~100MB；设上限避免被 OOM Killer |
| `-XX:+UseG1GC` | 低延迟 GC，适合请求响应型负载 |
| `-XX:MaxGCPauseMillis=50` | 单次 GC 暂停 < 50ms（避免 stdio JSON-RPC 卡住） |
| **`-Dfile.encoding=UTF-8`** | ⭐ **必填**——MCP SDK 0.18.x 用默认 charset 读 stdin；Windows 默认 GBK 会导致 UTF-8 中文 3 字节被解码成 1-2 garbled chars → Content-Length 错位 → 死锁 |
| `-XX:+HeapDumpOnOutOfMemoryError` | 内存问题时自动 dump 到 `/var/log/sql-forge-mcp/` 便于事后分析 |
| `--spring.main.web-application-type=none` | 不启 Tomcat（stdio MCP 不需要 web 容器） |

### 2.2 内存预估

| 组件 | 占用 |
|------|------|
| JVM 基础 | ~50MB |
| Spring Boot + spring-ai | ~50MB |
| 业务缓存（54 组件 + 17 范例） | ~5MB |
| 每次 Tool 调用的瞬时对象 | < 1MB |
| **MCP server 进程总计** | **~100-120MB** |
| httpclient5 连接池（如果启用） | ~30MB |
| Playwright Chromium 子进程（可选） | ~150MB |

推荐：`-Xmx512m` 给 stdio-only 场景，启用 Playwright 时 `-Xmx768m`。

## 3. Claude Code / mcp-cli 集成配置

### 3.1 `.mcp.json`（stdio transport）

```json
{
  "mcpServers": {
    "sql-forge-mcp": {
      "command": "java",
      "args": [
        "--java-options=-Dfile.encoding=UTF-8",
        "--java-options=-Dsun.jnu.encoding=UTF-8",
        "--java-options=-Xmx512m",
        "-jar", "/opt/sql-forge-mcp/sql-forge-mcp.jar",
        "--sql.forge.mcp.systems[0].name=Prod",
        "--sql.forge.mcp.systems[0].url=http://sql-forge-backend:8081",
        "--sql.forge.mcp.systems[0].apiKey=${API_KEY}",
        "--sql.forge.mcp.systems[0].description=生产环境业务后端",
        "--spring.main.web-application-type=none"
      ],
      "env": {
        "API_KEY": "secret-from-vault"
      }
    }
  }
}
```

> ⚠️ **`--java-options=-Dfile.encoding=UTF-8` 必填**。不设就死锁（见 §2.1）。

### 3.2 API Key 管理

- � **不要** 写在 `.mcp.json` 静态文件里
- ✅ 用环境变量 `${API_KEY}` 注入
- ✅ 生产环境从 Vault / k8s Secret 注入
- ✅ Claude Code 的 `.mcp.json` 配合 `env` 字段自动展开

## 4. 日志规范

### 4.1 业务日志（SLF4J + Logback）

输出到 `/var/log/sql-forge-mcp/sql-forge-mcp-server.log`，**单文件、JSON 行格式**便于 ELK / Loki 抓取。

### 4.2 审计日志

每个 Tool 调用 → `./mcp/audit.log`（1KB 截断 args）。**合规要求**必须留 ≥ 90 天。

### 4.3 Logback 配置示例（保存为 `/etc/sql-forge-mcp/logback.xml`）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/sql-forge-mcp/sql-forge-mcp-server.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{ISO8601} %-5level [%thread] %logger - %msg%n</pattern>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="FILE"/>
    </root>
    <logger name="cn.wubo.sql.forge" level="DEBUG"/>
    <logger name="com.microsoft.playwright" level="WARN"/>
</configuration>
```

## 5. 监控建议

### 5.1 核心指标（目前来源）

sql-forge-mcp 本身**不暴露 HTTP** 走 Prometheus 端点（见 R5-2 决策）。当前的可观测性靠 `metrics()` 和 `mcpHealth()` 两个 Tool 返回：

```bash
# Claude Code 调用：
mcp: tools/call metrics '{}'
# 返回：
{
  "startedAtMs": 1756234567,
  "uptimeSec": 86400,
  "tools": {
    "jsonSelect": {"calls": 1234, "errors": 5, "avgLatencyMs": 12, "maxLatencyMs": 450},
    "mcpHealth": {"calls": 60, "errors": 0, ...}
  }
}

mcp: tools/call mcpHealth '{}'
# 返回：
{
  "status": "UP",
  "backends": {
    "Prod": {"status": "UP", "latencyMs": 23}
  },
  "playwright": {"status": "DISABLED"},
  "limits": {...}
}
```

### 5.2 监控告警阈值（基于 `metrics()` 数据）

| 指标 | 阈值 | 严重程度 |
|------|------|---------|
| `tools.*.errorRate > 5%`（任意 Tool） | 5 分钟内 | ⚠️ Warning |
| `backends.Prod.status = DOWN` | 30 秒 | 🔴 Critical |
| `mcpHealth.status = DEGRADED` | 30 秒 | 🔴 Critical |
| 单 Tool `avgLatencyMs > 1000` | 5 分钟 | ⚠️ Warning |
| 单 Tool `maxLatencyMs > 5000` | 单次 | ⚠️ Warning |

> 实际接入 Prometheus 需要先做 R5-2（HTTP transport），现在只能靠 Claude Code 主动调 `metrics()` / `mcpHealth()` 检查。

### 5.3 JVM 自带监控（无需 Prometheus 端点）

```bash
# 看 GC 情况
jcmd <PID> GC.heap_info

# 看线程栈（卡死时排错）
jcmd <PID> Thread.print

# 性能计数器
jcmd <PID> PerfCounter.print

# 远程连接 jstatd（启动时加 -Dcom.sun.management.jmxremote.port=9090）
jstat -gc <PID> 1s
```

## 6. 故障排查

### 6.1 症状速查

| 症状 | 根因 | 修复 |
|------|------|------|
| Tool 调用卡住不返回 | MCP stdio 字符集死锁 | 加 `--java-options=-Dfile.encoding=UTF-8` |
| Tool 返回 "无法连接后端" | 后端网络 / 进程 / API Key | `mcpHealth()` 看哪个 backend DOWN；查后端日志 |
| Tool 返回 "请求被后端拒绝（400）" | body 格式错 | 看 audit.log 对应调用 args |
| Tool 返回 "后端 500" | 后端 bug | 后端日志 + audit.log |
| `mcpHealth.status = DEGRADED` | 至少一个 backend 不可达 | `backends` 字段看哪个 DOWN |
| `metrics.tools.X.maxLatencyMs > 5000` | 某次调用卡住 | audit.log 看那次 args；查后端日志 |

### 6.2 排查流程

```
Tool 调用失败
├─ 看 mcpHealth → 后端都 UP？
│   ├─ YES → 不是网络问题
│   └─ NO → 后端网络/进程问题
│
├─ 看 metrics → errorRate 高？
│   ├─ YES → 大概率业务问题（SQL 错、字段错等）
│   └─ NO → 单次失败 → 看 audit.log 那次 args
│
├─ 看 maxLatencyMs → 大于 5s？
│   ├─ YES → 后端慢 SQL / 网络抖动
│   └─ NO → 同步失败（参数错、权限错）
│
└─ 看错误消息
    ├─ "认证失败" → 检查 X-Api-Key
    ├─ "后端路径不存在" → 检查 baseUrl 配置
    └─ "请求被后端拒绝（400）" → 检查 body 格式（用 validateAmisTemplate 验证）
```

## 7. 备份 & 恢复

### 7.1 备份内容

```bash
# 1. 审计日志（合规）
/var/log/sql-forge-mcp/audit.log

# 2. Amis 模板（持久化在业务后端数据库，跟着业务后端备份）
-- 通过 sql-forge 后端导出
SELECT * FROM SQL_FORGE_TEMPLATE_AMIS;

# 3. 配置（如果改了 mcp 系统配置）
.k8s/secret/sql-forge-mcp.yaml
```

### 7.2 灾难恢复

```bash
# 1. 部署新的 sql-forge-mcp pod
kubectl apply -f sql-forge-mcp-deployment.yaml

# 2. 启动后调 mcpHealth 验证
# Claude Code → /mcp reconnect → 调 mcpHealth

# 3. 业务后端模板数据从 SQL 备份恢复
psql -f template_amis_backup.sql

# 4. 验证 amisTemplateSave / amisTemplateList 工作
```

## 8. 滚动升级

### 8.1 灰度

```bash
# 50% 流量到 v1.1，50% 留 v1.0
kubectl patch deployment sql-forge-mcp -p '
spec:
  replicas: 2
  strategy:
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
'
# 观察 24h：
# - mcpHealth() 全 UP
# - metrics() errorRate < 0.1%
# - 没有 OOM / restart
# 通过后升到 100%
```

### 8.2 回滚

```bash
# 快速回滚到 v1.0
kubectl rollout undo deployment/sql-forge-mcp
# v1.0 jar 必须在镜像里保留（用 image tag 区分）
```

## 9. 安全

### 9.1 API Key 隔离

- 每个环境用不同 X-Api-Key（dev / staging / prod）
- key 至少 32 字符随机
- 定期轮换（每 90 天）
- 泄露立即吊销（后端 + MCP 双端同时改）

### 9.2 工具描述审计

每个 Tool 的 `@Tool(description = "...")` 是 AI Agent 决策依据。定期审计：
- 删除 Tool 是否描述准确
- 新增 Tool 是否有 `⚠️ DESTRUCTIVE:` 前缀（jsonDelete / deleteAmisTemplate / deleteSqlTemplate）

## 10. 检查清单（部署前/后）

### 部署前

```
□ JDK 17+ 装好
□ --java-options=-Dfile.encoding=UTF-8 在 .mcp.json
□ API Key 从 Vault / Secret 注入（不写死文件）
□ 后端 8081 可达 + X-Api-Key 校验通过
□ Playwright Chromium 已装（如需 preview）
□ 日志目录 /var/log/sql-forge-mcp/ 可写
□ 审计目录 ./mcp/audit.log 可写（相对进程 cwd）
□ JVM 参数：-Xmx512m / G1GC / HeapDumpOnOutOfMemoryError
```

### 部署后

```
□ mcpHealth() overall = UP
□ mcpHealth() backends.Prod = UP
□ mcpHealth() playwright 状态符合预期
□ 调一次 mcpHealth() + metrics() 有结果
□ /var/log/sql-forge-mcp/ 滚动日志正常
□ /var/log/sql-forge-mcp/audit.log 写入正常
□ Claude Code reconnect 后 Tool 列表 ≥ 28 个
□ 跑一次 test-mcp-e2e.sh（24 用例）全过
□ 跑一次 mvn test -Dtest='PlaywrightRenderTest'（3 用例，~25s，需要 Chromium）
```

---

**相关文档**：
- `docs/profiling.md` —— 性能分析工具与常见瓶颈
- `sql-forge-test/scripts/test-mcp-e2e.sh` —— 真实环境集成测试
- `sql-forge-test/scripts/test-mcp-chaos.sh` —— 混沌测试
- `sql-forge-test/scripts/test-mcp-loadtest.sh` —— 压测
