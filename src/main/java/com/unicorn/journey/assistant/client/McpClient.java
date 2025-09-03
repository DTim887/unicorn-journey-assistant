package com.unicorn.journey.assistant.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MCP客户端，用于调用MCP服务器接口
 */
@Service
public class McpClient {

    private static final Logger logger = LoggerFactory.getLogger(McpClient.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String mcpServerUrl;

    public McpClient(RestTemplate restTemplate, ObjectMapper objectMapper, 
                     @Value("${mcp.server.url:http://localhost:8002/unicorn-journey-assistant/system/rpc}") String mcpServerUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.mcpServerUrl = mcpServerUrl;
    }



    /**
     * 调用MCP接口检查退款资格
     * @param visitDate 入园日期
     * @return 退款资格检查结果
     */
    public McpResponse checkRefundEligibility(String visitDate) {
        Map<String, Object> params = new HashMap<>();
        params.put("visitDate", visitDate);
        return callMcpMethod("checkRefundEligibility", params);
    }

    /**
     * 调用MCP接口处理退款
     * @param orderId 订单ID
     * @param visitDate 入园日期
     * @return 退款处理结果
     */
    public McpResponse processRefund(String orderId, String visitDate) {
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", orderId);
        params.put("visitDate", visitDate);
        return callMcpMethod("processRefund", params);
    }

    /**
     * 通用的MCP方法调用
     * @param method 方法名
     * @param params 参数
     * @return MCP响应
     */
    private McpResponse callMcpMethod(String method, Map<String, Object> params) {
        try {
            // 创建JSON-RPC请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("jsonrpc", "2.0");
            requestBody.put("method", method);
            requestBody.put("params", params);
            requestBody.put("id", UUID.randomUUID().toString());

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            logger.info("Calling MCP method: {}", method);
            ResponseEntity<McpResponse> response = restTemplate.exchange(
                    mcpServerUrl,
                    HttpMethod.POST,
                    requestEntity,
                    McpResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("MCP call successful for method: {}", method);
                return response.getBody();
            } else {
                logger.error("MCP call failed with status code: {}", response.getStatusCode());
                McpResponse errorResponse = new McpResponse();
                errorResponse.setError(new McpError(-1, "MCP server returned non-OK status code"));
                return errorResponse;
            }
        } catch (Exception e) {
            logger.error("Error calling MCP method: {}", method, e);
            McpResponse errorResponse = new McpResponse();
            errorResponse.setError(new McpError(-1, "Failed to call MCP server: " + e.getMessage()));
            return errorResponse;
        }
    }

    /**
     * MCP响应实体类
     */
    public static class McpResponse {
        private Object result;
        private McpError error;
        private String id;

        // getters and setters
        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
        }

        public McpError getError() {
            return error;
        }

        public void setError(McpError error) {
            this.error = error;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    /**
     * MCP错误实体类
     */
    public static class McpError {
        private int code;
        private String message;
        private String details;

        public McpError(int code, String message) {
            this.code = code;
            this.message = message;
        }

        // getters and setters
        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }
    }
}