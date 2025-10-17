package com.unicorn.journey.assistant.langgraph.demo;

public class SimpleAgentApp {

    // 1. 定义 Agent 的状态 (State)
    // 这是 Agent 的“短期记忆”，在图的各个节点之间传递。
    // 使用 Record 可以方便地创建不可变的状态对象。
//    public record AgentState(List<ChatMessage> messages) {
//        public AgentState addMessage(ChatMessage message) {
//            List<ChatMessage> newMessages = new ArrayList<>(messages);
//            newMessages.add(message);
//            return new AgentState(newMessages);
//        }
//    }


    public static void main(String[] args) {
//        AiAgent aiAgent = AiAgentFactory
//
//
//        // --- 定义图的节点 (Nodes) ---
//
//        // 节点 1: "agent" - 负责思考和推理
//        // 这个节点调用 LLM，LLM 会根据当前消息历史决定是直接回答还是调用工具。
//        Function<AgentState, AgentState> agentNode = (state) -> {
//            System.out.println("[Agent Node] Thinking...");
//            ChatRequest chatRequest = ChatRequest.builder().messages(new ChatMessage[]{UserMessage.from("111")}).build();
//            ChatResponse chatResponse = chatModel.chat(chatRequest);
////            AiMessage response = model.generate(state.messages(), weatherTool).content();
//            return state.addMessage(chatResponse.aiMessage());
//        };
//
//        // 节点 2: "tool" - 负责执行工具
//        // 如果 "agent" 节点决定调用工具，流程就进入这个节点。
//        Function<AgentState, AgentState> toolNode = (state) -> {
//            System.out.println("[Tool Node] Executing tool...");
//            AiMessage lastMessage = (AiMessage) state.messages().getLast();
//            ToolExecutionRequest toolRequest = lastMessage.toolExecutionRequests().getFirst();
//
//            // 实际执行工具方法
//            String toolResult = weatherTool.getCurrentWeather(toolRequest.argumentsAsMap().get("city").toString());
//            ToolExecutionResultMessage toolMessage = ToolExecutionResultMessage.from(toolRequest, toolResult);
//
//            return state.addMessage(toolMessage);
//        };
//
//
//        // --- 定义条件路由 (Conditional Edge) ---
//        // 这是实现“规划”和“循环”的核心。
//        // 它检查 Agent 的最新回复，并决定下一步去哪里。
//        Function<AgentState, String> router = (state) -> {
//            ChatMessage lastMessage = state.messages().get(state.messages().size() - 1);
//            if (lastMessage instanceof AiMessage && ((AiMessage) lastMessage).hasToolExecutionRequests()) {
//                // 如果 LLM 请求调用工具，则路由到 "tool" 节点
//                System.out.println("[Router] Decision: Use tool.");
//                return "tool";
//            }
//            // 否则，流程结束
//            System.out.println("[Router] Decision: Finish.");
//            return StateGraph.END;
//        };
//
//
//        // --- 构建图 (Graph) ---
//        GraphState<AgentState> graphState = new GraphState<>(AgentState.class);
//        Graph.Builder<AgentState> builder = new Graph.Builder<>(graphState);
//
//        builder.setEntryPoint("agent"); // 设置入口点
//        builder.addNode("agent", agentNode); // 添加 agent 节点
//        builder.addNode("tool", toolNode);   // 添加 tool 节点
//
//        // 从 "agent" 节点出来后，使用 router 来决定下一步
//        builder.addConditionalEdges("agent", router, Map.of("tool", "tool", Graph.END, Graph.END));
//
//        // "tool" 节点执行完毕后，必须回到 "agent" 节点，让它带着新的信息再次思考
//        builder.addEdge("tool", "agent");
//
//        Graph<AgentState> graph = builder.build();
//        AgentExecutor<AgentState> agent = new AgentExecutor<>(graph);
//
//
//        // --- 运行 Agent ---
//        String userInput = "上海今天天气怎么样？";
//        System.out.println("User: " + userInput);
//
//        // 初始状态
//        AgentState initialState = new AgentState(Collections.singletonList(UserMessage.from(userInput)));
//
//        // 使用 stream() 来观察每一步的执行过程
//        System.out.println("\n--- Agent Execution Flow ---");
//        agent.stream(initialState).forEach(step -> {
//            String nodeName = step.getNodeName();
//            AgentState currentState = step.getState();
//            ChatMessage lastMessage = currentState.messages().get(currentState.messages().size() - 1);
//            System.out.printf("--- Step: Executed Node '%s' ---%n", nodeName);
//            System.out.println("  Last Message: " + lastMessage.text());
//            System.out.println("  Full Message History Size: " + currentState.messages().size());
//            System.out.println("---------------------------\n");
//        });
    }
}