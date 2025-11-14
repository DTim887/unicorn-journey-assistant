package com.unicorn.journey.assistant.langgragh4j.hotel.state;

import com.unicorn.journey.assistant.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 酒店智能助手工作流上下文
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelAssistantContext implements Serializable {
    @Serial
    private static final long serialVersionUID = 123456L;

    /**
     * Context 在 MessagesState 中的存储key
     */
    public static final String CONTEXT_KEY = "hotelAssistantContext";

    /**
     * 当前执行步骤
     */
    private String currentStep;

    /**
     * 用户
     */
    private User user;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户输入的原始消息
     */
    private String userMessage;

    /**
     * 对话历史记录（所有用户输入）
     */
    @Builder.Default
    private List<String> conversationHistory = new ArrayList<>();

    /**
     * 是否需要用户确认
     */
    private boolean needConfirmation;

    /**
     * 确认类型 (MENU / ROOM_BOOKING / WAKE_UP / CONCIERGE 等)
     */
    private String confirmationType;

    /**
     * 用户确认结果 (approved / rejected / regenerate)
     */
    private String confirmationResult;

    /**
     * 暂停时所在的节点名称
     */
    private String pausedAtNode;

    /**
     * 是否为恢复执行模式
     */
    private boolean isResuming;

    /**
     * 任务列表 (待执行的任务)
     */
    @Builder.Default
    private List<AgentTask> tasks = new ArrayList<>();

    /**
     * 当前任务索引
     */
    @Builder.Default
    private int currentTaskIndex = 0;

    /**
     * 任务执行结果存储 (key: taskId, value: 任务执行结果)
     */
    @Builder.Default
    private Map<String, String> taskResults = new HashMap<>();

    /**
     * 子 Agent 参数存储 (用于收集用户输入的参数)
     */
    @Builder.Default
    private Map<String, Object> agentParams = new HashMap<>();

    /**
     * 最终汇总信息
     */
    private String summary;

    // ========== 上下文操作方法 ==========

    /**
     * 从 MessagesState 中获取 Context
     */
    public static HotelAssistantContext getContext(MessagesState<String> state) {
        return (HotelAssistantContext) state.data().get(CONTEXT_KEY);
    }

    /**
     * 将 Context 保存到 MessagesState 中
     */
    public static Map<String, Object> saveContext(HotelAssistantContext context) {
        return Map.of(CONTEXT_KEY, context);
    }

    /**
     * 获取当前任务
     */
    public AgentTask getCurrentTask() {
        if (tasks.isEmpty() || currentTaskIndex >= tasks.size()) {
            return null;
        }
        return tasks.get(currentTaskIndex);
    }

    /**
     * 是否还有待执行的任务
     */
    public boolean hasMoreTasks() {
        return currentTaskIndex < tasks.size();
    }

    /**
     * 移动到下一个任务
     */
    public void moveToNextTask() {
        currentTaskIndex++;
    }

    /**
     * 添加用户输入到对话历史
     */
    public void addUserInput(String input) {
        if (input != null && !input.trim().isEmpty()) {
            conversationHistory.add(input);
        }
    }

    /**
     * 获取对话历史文本
     */
    public String getConversationHistoryText() {
        if (conversationHistory.isEmpty()) {
            return "";
        }
        return String.join("\n", conversationHistory);
    }

    /**
     * 任务类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AgentTask implements Serializable {
        @Serial
        private static final long serialVersionUID = 456789L;

        /**
         * 任务ID
         */
        private String taskId;

        /**
         * 任务类型 (对应子 Agent 类型: MENU_ORDER, ROOM_BOOKING, WAKE_UP_CALL, CONCIERGE_SERVICE 等)
         */
        private String taskType;

        /**
         * 任务描述
         */
        private String description;

        /**
         * 任务状态 (PENDING, RUNNING, COMPLETED, FAILED)
         */
        @Builder.Default
        private String status = "PENDING";

        /**
         * 任务所需参数
         */
        @Builder.Default
        private Map<String, Object> params = new HashMap<>();

        /**
         * 任务执行结果
         */
        private String result;
    }
}
