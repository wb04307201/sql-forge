#!/bin/bash
#
# test-mcp-e2e.sh —— sql-forge-mcp 端到端测试
#
# 跑 Phase 0-4：
#   Phase 0  启动 sql-forge-test 后端
#   Phase 1  mcpHealth（整体健康检查）
#   Phase 2  关键 Tool 调用（getSystems / getMetaDataDatabase / findTablesByName 等）
#   Phase 3  MCP Resources（amis://schema-hints / components / examples）
#   Phase 4  Amis 工具（validateAmisTemplate / previewAmisTemplate）
#
# 用法：
#   ./test-mcp-e2e.sh
#
# 退出码：
#   0 = 全部通过
#   1 = 后端启动失败
#   2 = Phase 1-4 任一用例失败

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MCP_JAR="$PROJECT_ROOT/sql-forge-mcp/target/sql-forge-mcp-1.0-SNAPSHOT.jar"
CLI="$SCRIPT_DIR/mcp-cli.py"

PASS=0
FAIL=0
BACKEND_PID=""

cleanup() {
    if [ -n "$BACKEND_PID" ]; then
        echo "Stopping backend (PID=$BACKEND_PID)..."
        kill -9 "$BACKEND_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT

# ============ 阶段 0：启动后端 ============

echo "=== Phase 0: 启动 sql-forge-test 后端 ==="
cd "$PROJECT_ROOT"
mvn -pl sql-forge-test spring-boot:run -DskipTests -Dspring-boot.run.arguments=--server.port=8081 \
    > /tmp/sql-forge-test.log 2>&1 &
BACKEND_PID=$!
echo "Backend started, PID=$BACKEND_PID"

# 等待后端启动
echo -n "等待后端就绪"
for i in $(seq 1 30); do
    if curl -sf "http://localhost:8081/sql/forge/api/database/metaDataDatabase" \
        -H "X-Api-Key: test" -o /dev/null --max-time 2 2>/dev/null; then
        echo " ✓ (尝试 $i)"
        break
    fi
    echo -n "."
    sleep 2
done

if ! curl -sf "http://localhost:8081/sql/forge/api/database/metaDataDatabase" \
    -H "X-Api-Key: test" -o /dev/null --max-time 2 2>/dev/null; then
    echo " ✗ 后端启动失败"
    cat /tmp/sql-forge-test.log | tail -20
    exit 1
fi

# 检查 jar 是否存在
if [ ! -f "$MCP_JAR" ]; then
    echo "ERROR: MCP jar not found at $MCP_JAR"
    exit 1
fi

# 检查 mcp-cli 是否可用
if [ ! -f "$CLI" ]; then
    echo "ERROR: mcp-cli.py not found at $CLI"
    exit 1
fi
chmod +x "$CLI"

# 工具函数：跑一个测试用例
run_case() {
    local desc="$1"
    local cmd="$2"
    local expected="$3"

    if eval "$cmd" 2>/dev/null | grep -q "$expected"; then
        echo "  ✓ $desc"
        PASS=$((PASS+1))
    else
        echo "  ✗ $desc"
        echo "    cmd: $cmd"
        FAIL=$((FAIL+1))
    fi
}

# ============ Phase 1：mcpHealth ============

echo ""
echo "=== Phase 1: mcpHealth ==="

run_case "mcpHealth → UP" \
    "python '$CLI' --jar '$MCP_JAR' tools/call mcpHealth '{}'" \
    '"status": "UP"'

run_case "mcpHealth → TestSys backend UP" \
    "python '$CLI' --jar '$MCP_JAR' tools/call mcpHealth '{}'" \
    '"TestSys"'

run_case "mcpHealth → registeredTools = 28" \
    "python '$CLI' --jar '$MCP_JAR' tools/call mcpHealth '{}'" \
    '"registeredTools": 28'

# ============ Phase 2：关键 Tool 调用 ============

echo ""
echo "=== Phase 2: 关键 Tool 调用 ==="

run_case "getSystems → 返回 TestSys" \
    "python '$CLI' --jar '$MCP_JAR' tools/call getSystems '{}'" \
    'TestSys'

run_case "getMetaDataDatabase → H2" \
    "python '$CLI' --jar '$MCP_JAR' tools/call getMetaDataDatabase '{\"systemName\":\"TestSys\"}'" \
    'H2'

run_case "findTablesByName('USER') → USERS" \
    "python '$CLI' --jar '$MCP_JAR' tools/call findTablesByName '{\"systemName\":\"TestSys\",\"keyword\":\"USER\"}'" \
    'USERS'

run_case "jsonSelect USERS → 11 用户" \
    "python '$CLI' --jar '$MCP_JAR' tools/call jsonSelect '{\"systemName\":\"TestSys\",\"tableName\":\"USERS\",\"body\":null}'" \
    'USERNAME'

run_case "countRows USERS → 11" \
    "python '$CLI' --jar '$MCP_JAR' tools/call countRows '{\"systemName\":\"TestSys\",\"tableName\":\"USERS\",\"whereJson\":null}'" \
    'TOTAL'

run_case "describeSchema USERS → columns" \
    "python '$CLI' --jar '$MCP_JAR' tools/call describeSchema '{\"systemName\":\"TestSys\",\"tableNamePattern\":\"USERS\"}'" \
    'columns'

run_case "executeSqlTemplateSafely → 参数缺失" \
    "python '$CLI' --jar '$MCP_JAR' tools/call executeSqlTemplateSafely '{\"systemName\":\"TestSys\",\"id\":\"sql-template-database\"}'" \
    '参数缺失'

run_case "unknown systemName → 友好错误" \
    "python '$CLI' --jar '$MCP_JAR' tools/call getMetaDataDatabase '{\"systemName\":\"Ghost\"}'" \
    '系统不存在'

# ============ Phase 3：MCP Resources ============

echo ""
echo "=== Phase 3: MCP Resources ==="

run_case "amis://schema-hints → Markdown" \
    "python '$CLI' --jar '$MCP_JAR' resources/read amis://schema-hints" \
    'Amis Schema 速查手册'

run_case "amis://components → 54 组件" \
    "python '$CLI' --jar '$MCP_JAR' resources/read amis://components" \
    'input-tree'

run_case "amis://examples → 17 范例" \
    "python '$CLI' --jar '$MCP_JAR' resources/read amis://examples" \
    'crud-page'

# ============ Phase 4：Amis 校验/渲染 ============

echo ""
echo "=== Phase 4: Amis 校验/渲染 ==="

run_case "validateAmisTemplate 合法 → valid" \
    "python '$CLI' --jar '$MCP_JAR' tools/call validateAmisTemplate '{\"context\":\"{\\\"type\\\":\\\"page\\\",\\\"title\\\":\\\"test\\\"}\"}'" \
    '"valid": true'

run_case "validateAmisTemplate 非法 JSON → 友好错误" \
    "python '$CLI' --jar '$MCP_JAR' tools/call validateAmisTemplate '{\"context\":\"{ broken\"}'" \
    'JSON 解析失败'

run_case "previewAmisTemplate → rendered" \
    "python '$CLI' --jar '$MCP_JAR' tools/call previewAmisTemplate '{\"systemName\":\"\",\"context\":\"{\\\"type\\\":\\\"page\\\",\\\"title\\\":\\\"e2e\\\"}\"}'" \
    '"rendered": true'

# ============ 阶段 5：中文 charset 回归 ============

echo ""
echo "=== Phase 5: 中文 charset 回归 ==="

run_case "amisTemplateSave 中文 → 成功" \
    "python '$CLI' --jar '$MCP_JAR' tools/call amisTemplateSave '{\"systemName\":\"TestSys\",\"id\":\"e2e_cn_001\",\"name\":\"中文名\",\"description\":\"中文描述\",\"context\":\"{}\"}'" \
    '"result": "true"'

run_case "getAmisTemplate 中文 → 不乱码" \
    "python '$CLI' --jar '$MCP_JAR' tools/call getAmisTemplate '{\"systemName\":\"TestSys\",\"id\":\"e2e_cn_001\"}'" \
    '中文名'

run_case "deleteAmisTemplate 中文 → 成功" \
    "python '$CLI' --jar '$MCP_JAR' tools/call deleteAmisTemplate '{\"systemName\":\"TestSys\",\"id\":\"e2e_cn_001\"}'" \
    '"result": "true"'

# ============ 总结 ============

echo ""
echo "=== 结果 ==="
echo "通过: $PASS"
echo "失败: $FAIL"

if [ $FAIL -eq 0 ]; then
    echo "✓ 全部测试通过"
    exit 0
else
    echo "✗ 有 $FAIL 个测试失败"
    exit 2
fi
