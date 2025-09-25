package com.unicorn.journey.assistant.controller;

import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.JdkA2AHttpClient;
import io.a2a.client.transport.rest.RestTransport;
import io.a2a.client.transport.rest.RestTransportConfig;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TestController {

    @GetMapping("/test")
    public String test() throws A2AClientError, A2AClientException {
        try {
            // First, get the agent card for the A2A server agent you want to connect to
            AgentCard agentCard = new A2ACardResolver("http://localhost:8080/journey-assistant/judy-chat").getAgentCard();

            // Specify configuration for the ClientBuilder
            ClientConfig clientConfig = new ClientConfig.Builder()
                    .setAcceptedOutputModes(List.of("text"))
                    .build();

            // Create a custom HTTP client
            A2AHttpClient customHttpClient = new JdkA2AHttpClient();


            Client client = Client
                    .builder(agentCard)
                    .clientConfig(clientConfig)
                    .withTransport(RestTransport.class, new RestTransportConfig(customHttpClient))
                    .build();
            Message message = A2A.toUserMessage("tell me a joke");
            client.sendMessage(message);
        }catch (Exception e) {
            e.printStackTrace();
        }


        return "ok";
    }

}
