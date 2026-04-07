/*
* Copyright 2024 - 2024 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package cn.wubo.sql.forge.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;
import java.util.Map;

/**
 * With stdio transport, the MCP server is automatically started by the client. But you
 * have to build the server jar first:
 *
 * <pre>
 * ./mvnw clean install -DskipTests
 * </pre>
 */
@Slf4j
public class ClientStdio {

	public static void main(String[] args) {
		String jarPath = "./target/sql-forge-mcp-0.0.1-SNAPSHOT.jar";

		var stdioParams = ServerParameters.builder("java")
				.args("-jar", jarPath)
				.build();

		var transport = new StdioClientTransport(stdioParams, McpJsonMapper.createDefault());
		var client = McpClient.sync(transport).build();

		client.initialize();

		// List and demonstrate tools
		ListToolsResult toolsList = client.listTools();
		toolsList.tools().forEach(tool -> {
			log.info("Tool: {}", tool);
		});

		CallToolResult NL2SQLResult = client.callTool(new CallToolRequest("NL2SQL",
				Map.of("content", "查询订单里的所有商品,并按照商品统计数量")));

		log.info("NL2SQLResult: {}", NL2SQLResult);

		CallToolResult ExecuteSQLResult = client.callTool(new CallToolRequest("ExecuteSQL",
				Map.of("sql", "SELECT * FROM orders")));

		log.info("ExecuteSQLResult: {}", ExecuteSQLResult);


		CallToolResult GenerateJsonResult = client.callTool(new CallToolRequest("GenerateJson",
				Map.of("tableInfo", "[{\n" +
						"  \"table\": \"PRODUCTS\",\n" +
						"  \"desc\": \"商品表\",\n" +
						"  \"type\": \"crud\",\n" +
						"  \"fields\": {\n" +
						"    \"ID\": {\"type\": \"uuid\", \"desc\": \"商品ID\"},\n" +
						"    \"NAME\": {\"type\": \"string\", \"length\": 50, \"desc\": \"商品名称\",\"search\": true},\n" +
						"    \"DICT_CATEGORIES\": {\"type\": \"dict\", \"length\": 100, \"desc\": \"商品类型\", \"dict_code\": \"categories\", \"search\": true},\n" +
						"    \"PRICE\": {\"type\": \"number\", \"max\": 9999999999, \"precision\": 2, \"desc\": \"邮箱地址\", \"search\": true}\n" +
						"  }\n" +
						"},\n" +
						"  {\n" +
						"    \"table\": \"SYS_DICT_ITEMS\",\n" +
						"    \"desc\": \"字典项表\",\n" +
						"    \"type\": \"dict\",\n" +
						"    \"fields\": {\n" +
						"      \"DICT_CODE\": {\"type\": \"string\"},\n" +
						"      \"ITEM_CODE\": {\"type\": \"string\"},\n" +
						"      \"ITEM_NAME\": {\"type\": \"string\"}\n" +
						"    }\n" +
						"  }]")));

		log.info("GenerateJsonResult: {}", GenerateJsonResult);


		client.closeGracefully();
	}

}
