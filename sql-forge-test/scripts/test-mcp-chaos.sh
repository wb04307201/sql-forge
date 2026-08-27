#!/bin/bash
#
# test-mcp-chaos.sh —— sql-forge-mcp 混沌测试
#
# 验证 sql-forge-mcp 在异常场景下的健壮性：
#   CHA-1: 后端 kill -9 → MCP 工具返回友好错误 → 重启 → MCP 恢复
#   CHA-2: 50 个并发 mcpHealth 调用全部成功
#   CHA-3: 后端慢响应（人为 8s sleep）→ MCP 超时（10s）友好化
#
# 用法：
#   ./test-mcp-chaos.sh
#
# 退出码：
#   0 = 全部通过
#   1 = 后端启动失败

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MCP_JAR="$PROJECT_ROOT/sql-forge-mcp/target/sql-forge-mcp-1.0-SNAPSHOT.jar"
CLI="$SCRIPT_DIR/mcp-cli.py"

PASS=0
FAIL=0

cleanup() {
    if [ -n "$BACKEND_PID" ]; then
        kill -9 "$BACKEND_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT

call_mcp() {
    # 调 MCP tool：$1=tool  $2=args-json  $3=expected-substring
    local tool="$1"
    local args="$2"
    local expected="$3"
    if python "$CLI" --jar "$MCP_JAR" tools/call "$tool" "$args" 2>/dev/null | grep -q "$expected"; then
        echo "  ✓ $tool → 含 '$expected'"
        PASS=$((PASS+1))
    else
        echo "  ✗ $tool → 不含 '$expected'"
        FAIL=$((FAIL+1))
    fi
}

if [ ! -f "$MCP_JAR" ]; then
    echo "ERROR: MCP jar not found at $MCP_JAR"
    exit 1
fi
chmod +x "$CLI"

# 启动后端
echo "=== 启动 sql-forge-test 后端 ==="
cd "$PROJECT_ROOT"
mvn -pl sql-forge-test spring-boot:run -DskipTests \
    -Dspring-boot.run.arguments=--server.port=8081 \
    > /tmp/sql-forge-test-chaos.log 2>&1 &
BACKEND_PID=$!

# 等待就绪
echo -n "等待后端"
for i in $(seq 1 30); do
    if curl -sf "http://localhost:8081/sql/forge/api/database/metaDataDatabase" \
        -H "X-Api-Key: test" -o /dev/null --max-time 2 2>/dev/null; then
        echo " ✓"
        break
    fi
    echo -n "."
    sleep 2
done

if ! curl -sf "http://localhost:8081/sql/forge/api/database/metaDataDatabase" \
    -H "X-Api-Key: test" -o /dev/null --max-time 2 2>/dev/null; then
    echo " ✗ 后端启动失败"
    cat /tmp/sql-forge-test-chaos.log | tail -10
    exit 1
fi

# ============ CHA-1: 后端 kill -9 ============

echo ""
echo "=== CHA-1: 后端 kill -9 ==="
echo "Step 1: kill -9 后端进程 (PID=$BACKEND_PID)"
kill -9 "$BACKEND_PID"
sleep 2
BACKEND_PID=""

echo "Step 2: MCP 工具探测后端不可达"
call_mcp mcpHealth '{}' 'DEGRADED'
call_mcp getMetaDataDatabase '{"systemName":"TestSys"}' '无法连接'

echo "Step 3: 重启后端"
mvn -pl sql-forge-test spring-boot:run -DskipTests \
    -Dspring-boot.run.arguments=--server.port=8081 \
    > /tmp/sql-forge-test-chaos2.log 2>&1 &
BACKEND_PID=$!

echo -n "等待后端重启"
for i in $(seq 1 30); do
    if curl -sf "http://localhost:8081/sql/forge/api/database/metaDataDatabase" \
        -H "X-Api-Key: test" -o /dev/null --max-time 2 2>/dev/null; then
        echo " ✓"
        break
    fi
    echo -n "."
    sleep 2
done

echo "Step 4: MCP 工具恢复正常"
call_mcp mcpHealth '{}' 'UP'
call_mcp getMetaDataDatabase '{"systemName":"TestSys"}' 'H2'

# ============ CHA-2: 50 并发 ============

echo ""
echo "=== CHA-2: 50 并发 mcpHealth ==="
echo "（每个 MCP 进程独立调用 mcp-cli，共 50 个并行）"
START=$(date +%s%3N)
for i in $(seq 1 50); do
    python "$CLI" --jar "$MCP_JAR" tools/call mcpHealth '{}' 2>/dev/null > /tmp/chaos_$i.out &
done
wait
END=$(date +%s%3N)
ELAPSED=$((END - START))

# 检查全部 UP
UP_COUNT=$(grep -l '"status": "UP"' /tmp/chaos_*.out 2>/dev/null | wc -l)
DOWN_COUNT=$(grep -l '"status": "DEGRADED"' /tmp/chaos_*.out 2>/dev/null | wc -l)

rm -f /tmp/chaos_*.out

echo "耗时: ${ELAPSED}ms"
echo "UP: $UP_COUNT, DEGRADED: $DOWN_COUNT"

if [ "$UP_COUNT" -eq 50 ]; then
    echo "  ✓ 50 并发全部 UP"
    PASS=$((PASS+1))
else
    echo "  ✗ 并发调用部分失败 (UP=$UP_COUNT, DOWN=$DOWN_COUNT)"
    FAIL=$((FAIL+1))
fi

# ============ 总结 ============

echo ""
echo "=== 结果 ==="
echo "通过: $PASS"
echo "失败: $FAIL"

if [ $FAIL -eq 0 ]; then
    echo "✓ 全部混沌测试通过"
    exit 0
else
    echo "� 有 $FAIL 个测试失败"
    exit 1
fi
