package com.unicorn.journey.assistant.config;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class MCPClientConfiguration {


    @Value("${mcp.refund.sse-url}")
    private String sseUrl;

//    @Bean
    public McpToolProvider mcpToolProvider() {

        McpTransport mcpTransport = new HttpMcpTransport.Builder()
                .sseUrl(sseUrl)
                .timeout(Duration.ofSeconds(30))
                .logRequests(true)
                .logResponses(true)
                .build();

        McpClient mcpClient = new DefaultMcpClient.Builder()
                .key("refundMcpClient")
//                .logHandler(McpLogMessageHandler)
                .transport(mcpTransport)
                .build();

        return new McpToolProvider.Builder()
                .mcpClients(mcpClient)
                .build();
    }
}
