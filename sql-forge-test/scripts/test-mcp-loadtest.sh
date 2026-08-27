#!/bin/bash
#
# test-mcp-loadtest.sh —— sql-forge-mcp 压测脚本
#
# 用法：
#   ./test-mcp-loadtest.sh                       # 默认：50 并发 × 100 请求
#   ./test-mcp-loadtest.sh --users 100 --requests 500
#   ./test-mcp-loadtest.sh --tool jsonSelect --table USERS
#
# 输出：
#   - 实时进度（每秒一次）
#   - 完成后输出 P50/P95/P99 延迟 + 吞吐量
#
# 退出码：
#   0 = 全部成功
#   1 = 后端启动失败
#   2 = P95 超阈值

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MCP_JAR="$PROJECT_ROOT/sql-forge-mcp/target/sql-forge-mcp-1.0-SNAPSHOT.jar"
CLI="$SCRIPT_DIR/mcp-cli.py"

# 默认参数
USERS=50
REQUESTS=100
TOOL="mcpHealth"
ARGS='{}'
TABLE="USERS"
P95_THRESHOLD_MS=200   # 真实环境的 P95 阈值（含 MCP 进程冷启动）

# 解析参数
while [ $# -gt 0 ]; do
    case "$1" in
        --users) USERS="$2"; shift 2 ;;
        --requests) REQUESTS="$2"; shift 2 ;;
        --tool) TOOL="$2"; shift 2 ;;
        --table) TABLE="$2"; shift 2 ;;
        --p95-threshold) P95_THRESHOLD_MS="$2"; shift 2 ;;
        *) echo "Unknown arg: $1"; exit 1 ;;
    esac
done

# 根据 tool 设置 args
case "$TOOL" in
    jsonSelect) ARGS="{\"systemName\":\"TestSys\",\"tableName\":\"$TABLE\",\"body\":null}" ;;
    jsonSelectPage) ARGS="{\"systemName\":\"TestSys\",\"tableName\":\"$TABLE\",\"body\":{}}" ;;
    getMetaDataDatabase) ARGS="{\"systemName\":\"TestSys\"}" ;;
    findTablesByName) ARGS="{\"systemName\":\"TestSys\",\"keyword\":\"$TABLE\"}" ;;
    mcpHealth) ARGS='{}' ;;
    *) echo "Unsupported tool: $TOOL"; exit 1 ;;
esac

PASS=0
FAIL=0
BACKEND_PID=""

cleanup() {
    if [ -n "$BACKEND_PID" ]; then
        kill -9 "$BACKEND_PID" 2>/dev/null || true
    fi
    # 清理临时文件
    rm -f /tmp/loadtest_*.out
}
trap cleanup EXIT

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
    > /tmp/sql-forge-test-load.log 2>&1 &
BACKEND_PID=$!

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
    cat /tmp/sql-forge-test-load.log | tail -10
    exit 1
fi

# ============ 预热：跑 5 次（避免冷启动影响数据） ============

echo "=== 预热（5 次）==="
for i in 1 2 3 4 5; do
    python "$CLI" --jar "$MCP_JAR" tools/call "$TOOL" "$ARGS" > /dev/null 2>&1 || true
done

# ============ 主负载测试 ============

echo ""
echo "=== 负载测试：$USERS 并发 × $REQUESTS 请求，工具 $TOOL ==="

RESULT_DIR="/tmp/loadtest_$$"
mkdir -p "$RESULT_DIR"

START=$(date +%s%3N)
for i in $(seq 1 $REQUESTS); do
    (
        start_ms=$(date +%s%3N)
        if python "$CLI" --jar "$MCP_JAR" tools/call "$TOOL" "$ARGS" > /dev/null 2>&1; then
            end_ms=$(date +%s%3N)
            elapsed=$((end_ms - start_ms))
            echo "$elapsed" > "$RESULT_DIR/${i}.latency"
            echo "OK" > "$RESULT_DIR/${i}.status"
        else
            echo "FAIL" > "$RESULT_DIR/${i}.status"
        fi
    ) &
    # 控制并发数
    if [ $((i % USERS)) -eq 0 ]; then
        wait
    fi
done
wait
END=$(date +%s%3N)
ELAPSED_MS=$((END - START))

# ============ 统计结果 ============

echo ""
echo "=== 结果统计 ==="

# 成功/失败计数
OK_COUNT=$(grep -l "^OK$" "$RESULT_DIR"/*.status 2>/dev/null | wc -l)
FAIL_COUNT=$(grep -l "^FAIL$" "$RESULT_DIR"/*.status 2>/dev/null | wc -l)
TOTAL=$((OK_COUNT + FAIL_COUNT))

# 收集延迟
LATENCY_FILE="$RESULT_DIR/_all_latencies.txt"
cat "$RESULT_DIR"/*.latency 2>/dev/null | sort -n > "$LATENCY_FILE" || true
LATENCY_COUNT=$(wc -l < "$LATENCY_FILE")

if [ "$LATENCY_COUNT" -eq 0 ]; then
    echo "ERROR: 没有成功请求"
    exit 1
fi

# 计算分位数
P50=$(awk -v n="$LATENCY_COUNT" 'NR==int(n*0.50)+1' "$LATENCY_FILE")
P95=$(awk -v n="$LATENCY_COUNT" 'NR==int(n*0.95)+1' "$LATENCY_FILE")
P99=$(awk -v n="$LATENCY_COUNT" 'NR==int(n*0.99)+1' "$LATENCY_FILE")
MAX=$(tail -1 "$LATENCY_FILE")
MIN=$(head -1 "$LATENCY_FILE")
AVG=$(awk '{sum+=$1; n++} END {if(n>0) print int(sum/n); else print 0}' "$LATENCY_FILE")

# 吞吐量
THROUGHPUT_RPS=$((TOTAL * 1000 / ELAPSED_MS))

echo "总请求:  $TOTAL"
echo "成功:    $OK_COUNT"
echo "失败:    $FAIL_COUNT"
echo "耗时:    ${ELAPSED_MS}ms"
echo ""
echo "延迟（ms）："
echo "  min: $MIN"
echo "  avg: $AVG"
echo "  P50: $P50"
echo "  P95: $P95"
echo "  P99: $P99"
echo "  max: $MAX"
echo ""
echo "吞吐量：$THROUGHPUT_RPS req/s"

# ============ 性能门禁 ============

EXIT_CODE=0
if [ "$FAIL_COUNT" -gt 0 ]; then
    FAIL_RATE=$((FAIL_COUNT * 100 / TOTAL))
    echo ""
    echo "✗ 失败率 ${FAIL_RATE}%（${FAIL_COUNT}/${TOTAL}）"
    EXIT_CODE=1
fi

if [ "$P95" -gt "$P95_THRESHOLD_MS" ]; then
    echo ""
    echo "✗ P95 性能门禁失败：${P95}ms > 阈值 ${P95_THRESHOLD_MS}ms"
    EXIT_CODE=2
fi

if [ "$EXIT_CODE" -eq 0 ]; then
    echo ""
    echo "✓ 压测通过（$OK_COUNT/$TOTAL 成功，P95 ${P95}ms）"
fi

# 清理临时文件
rm -rf "$RESULT_DIR"

exit $EXIT_CODE
