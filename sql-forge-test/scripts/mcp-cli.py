#!/usr/bin/env python3
"""
mcp-cli —— 通过 stdio JSON-RPC 与 sql-forge-mcp server 通信的命令行客户端。
用于 test-mcp-e2e.sh 跑真实环境集成测试。

用法：
    python mcp-cli.py tools/list
    python mcp-cli.py tools/call <tool_name> '{...args...}'
    python mcp-cli.py resources/read <uri>
    python mcp-cli.py prompts/get <name> '{...args...}'
    python mcp-cli.py --jar <jar_path> tools/call mcpHealth '{}'
"""
import json
import subprocess
import sys
import argparse
import os
import uuid
import time


def send_request(proc, method, params=None, msg_id=None):
    """向 MCP server 发送一条 JSON-RPC 请求，等待响应。"""
    if msg_id is None:
        msg_id = str(uuid.uuid4())
    msg = {"jsonrpc": "2.0", "id": msg_id, "method": method}
    if params is not None:
        msg["params"] = params
    body = json.dumps(msg, ensure_ascii=False)
    proc.stdin.write(f"Content-Length: {len(body.encode('utf-8'))}\r\n\r\n{body}".encode("utf-8"))
    proc.stdin.flush()

    # 读 Content-Length 头
    headers = {}
    while True:
        line = proc.stdin.readline() if False else None  # placeholder
        line_bytes = proc.stdout.readline()
        if not line_bytes:
            return None
        line = line_bytes.decode("utf-8", errors="replace").rstrip("\r\n")
        if line == "":
            break
        if ":" in line:
            k, v = line.split(":", 1)
            headers[k.strip().lower()] = v.strip()
    content_length = int(headers.get("content-length", "0"))
    if content_length == 0:
        return None
    body_bytes = proc.stdout.read(content_length)
    return json.loads(body_bytes.decode("utf-8"))


def call_tool(proc, tool_name, args_dict):
    return send_request(proc, "tools/call", {"name": tool_name, "arguments": args_dict})


def main():
    ap = argparse.ArgumentParser(description="MCP stdio JSON-RPC CLI")
    ap.add_argument("--jar", default="./sql-forge-mcp/target/sql-forge-mcp-1.0-SNAPSHOT.jar",
                    help="MCP server jar 路径（默认相对路径）")
    ap.add_argument("--system-name", default="TestSys")
    ap.add_argument("--system-url", default="http://localhost:8081")
    ap.add_argument("--system-key", default="test")
    ap.add_argument("command", choices=["tools/list", "tools/call", "resources/read", "prompts/get"])
    ap.add_argument("target", help="tool 名称 / resource URI / prompt 名称")
    ap.add_argument("args", nargs="?", default="{}", help="JSON 字符串（tool args / prompt args）")
    args = ap.parse_args()

    if not os.path.isabs(args.jar):
        jar_path = os.path.join(os.getcwd(), args.jar)
    else:
        jar_path = args.jar
    if not os.path.exists(jar_path):
        print(f"ERROR: jar not found: {jar_path}", file=sys.stderr)
        sys.exit(1)

    # 启动 MCP server
    proc = subprocess.Popen(
        ["java",
         "--java-options=-Dfile.encoding=UTF-8",
         "--java-options=-Dsun.jnu.encoding=UTF-8",
         "-jar", jar_path,
         f"--sql.forge.mcp.systems[0].name={args.system_name}",
         f"--sql.forge.mcp.systems[0].url={args.system_url}",
         f"--sql.forge.mcp.systems[0].apiKey={args.system_key}",
         f"--sql.forge.mcp.systems[0].description=e2e"],
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
        bufsize=0
    )

    try:
        # 初始化（按 MCP 协议）
        time.sleep(1)  # 等 server 启动
        init_resp = send_request(proc, "initialize", {
            "protocolVersion": "2024-11-05",
            "capabilities": {},
            "clientInfo": {"name": "mcp-cli", "version": "1.0"}
        })
        if not init_resp or "error" in init_resp:
            print(f"ERROR: initialize failed: {init_resp}", file=sys.stderr)
            sys.exit(1)
        # 发送 initialized 通知（no response）
        note = {"jsonrpc": "2.0", "method": "notifications/initialized"}
        body = json.dumps(note)
        proc.stdin.write(f"Content-Length: {len(body.encode('utf-8'))}\r\n\r\n{body}".encode("utf-8"))
        proc.stdin.flush()

        # 执行命令
        if args.command == "tools/list":
            resp = send_request(proc, "tools/list")
        elif args.command == "tools/call":
            tool_args = json.loads(args.args) if args.args else {}
            resp = call_tool(proc, args.target, tool_args)
        elif args.command == "resources/read":
            resp = send_request(proc, "resources/read", {"uri": args.target})
        elif args.command == "prompts/get":
            prompt_args = json.loads(args.args) if args.args else {}
            resp = send_request(proc, "prompts/get", {"name": args.target, "arguments": prompt_args})

        print(json.dumps(resp, ensure_ascii=False, indent=2))
    finally:
        proc.terminate()
        try:
            proc.wait(timeout=3)
        except subprocess.TimeoutExpired:
            proc.kill()


if __name__ == "__main__":
    main()
