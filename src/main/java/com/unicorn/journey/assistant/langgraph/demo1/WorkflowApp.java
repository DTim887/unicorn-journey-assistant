package com.unicorn.journey.assistant.langgraph.demo1;

import com.unicorn.journey.assistant.langgraph.demo.SimpleState;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.Random;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class WorkflowApp {

    public Flux<String> execute() {
        return Flux.create(sink -> {
            Thread.startVirtualThread(() -> {
                StateGraph<SimpleState> graphDefinition = new StateGraph<>(SimpleState.SCHEMA, SimpleState::new);
                try {
                    graphDefinition.addNode("weather", node_async(new WeatherNode()));
                    graphDefinition.addNode("plan", node_async(new PlanNode()));
                    graphDefinition.addNode("plan2", node_async(new PlanNode2()));
                    graphDefinition.addEdge(StateGraph.START, "weather");
                    graphDefinition.addEdge("weather", "plan");
                    graphDefinition.addEdge("plan", "plan2");
                    graphDefinition.addEdge("plan2", StateGraph.END);
                    CompiledGraph<SimpleState> graph = graphDefinition.compile();
//         graph.invoke(Map.of( SimpleState.MESSAGES_KEY, "上海天气怎么样?" ));
                    int count = 1;
                    for (NodeOutput<SimpleState> item : graph.stream(Map.of(SimpleState.MESSAGES_KEY, "上海天气怎么样?<br>"))) {
                        if (!(item.isEND() || item.node().equals("plan2"))) {
                            log.info("APP: {} , Responder {}: ", item.node(), count + ": " + item.state().messages().getLast());
                            sink.next(count + ": " + item.state().messages().getLast());
                            count++;
                        }
                    }
                    sink.complete();
                } catch (GraphStateException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    public SseEmitter executeSse() {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        Thread.startVirtualThread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    Thread.sleep(Duration.ofMillis(100));
                    emitter.send(SseEmitter.event()
                            .name("event1")
                            .data(new Random(100)));
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    public Flux<String> executeAsync() {

        StateGraph<MyAgentState> graphDefinition = new StateGraph<>(MyAgentState.SCHEMA, MyAgentState::new);
        try {
            graphDefinition.addNode("first", node_async(new MyFirstNode()));
            graphDefinition.addNode("second", node_async(new MySecondNode()));
            graphDefinition.addEdge(StateGraph.START, "first");
            graphDefinition.addEdge("first", "second");
            graphDefinition.addEdge("second", StateGraph.END);
            CompiledGraph<MyAgentState> graph = graphDefinition.compile();
            Flux<String> startStream = Flux.just("--开始执行任务--<br>\n");
            Flux<String> executeStream = Flux.fromIterable(graph.stream(Map.of()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .filter(item -> !(item.isEND() || item.isSTART()))
                    .map(item -> item.state().messages().getLast() + "<br>");
            Flux<String> endStream = Flux.just("--所有任务都完成了--");
            return Flux.concat(startStream, executeStream, endStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


//        AiAgentFactory aiAgentFactory = SpringBeanUtils.getBean(AiAgentFactory.class);
//        AiAgent aiAgent = aiAgentFactory.createAiService(Assistants.WENNIE);
//        List<String> question = List.of("你好", "写个诗");
//        return Flux.fromIterable(question).concatMap(q -> {
//            Flux<String> a = Flux.just(String.format("<br> ##开始处理任务: [%s]<br>", q));
//            Flux<String> b = aiAgent.test("wennie_000", q);
//            return a.concatWith(b);
//        }).concatWith(Flux.just("--所有任务都完成了--"));
    }
}
