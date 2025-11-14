package com.unicorn.journey.assistant.langgragh4j.hotel.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 酒店路由调度 Agent
 * 负责解析用户需求，拆分任务，调度子 Agent
 */
public interface HotelRouterAgent {

    /**
     * 分析用户需求并生成任务列表
     * 返回格式: JSON 数组字符串
     * 
     * 示例返回:
     * [
     *   {"taskType": "MENU_ORDER", "description": "点餐服务", "params": {"mealType": "晚餐", "peopleCount": 2}},
     *   {"taskType": "WAKE_UP_CALL", "description": "叫醒服务", "params": {"wakeUpTime": "07:00"}}
     * ]
     */
    @SystemMessage("""
        你是一个酒店智能助手的任务调度路由 Agent。
        
        你的职责是：
        1. 理解用户的需求
        2. 将用户需求拆分成多个独立的任务
        3. 为每个任务分配对应的子 Agent 类型
        4. 识别任务所需的参数
        
        可用的子 Agent 类型：
        - MENU_ORDER: 点餐服务（需要参数: mealType餐食类型, peopleCount用餐人数, dietaryRestrictions饮食限制等）
        - ROOM_BOOKING: 客房预订服务（需要参数: roomType房型, checkInDate入住日期, nights入住天数等）
        - ROOM_RENEWAL: 续约房间服务（需要参数: roomNumber房间号, renewalDays续约天数, roomType房型等）
        - WAKE_UP_CALL: 叫醒服务（需要参数: wakeUpTime叫醒时间）
        - CONCIERGE_SERVICE: 礼宾服务（需要参数: serviceType服务类型, details详细需求）
        
        用户当前输入：{{userMessage}}
        
        请分析用户需求，生成任务列表。返回格式必须是 JSON 数组字符串，每个任务包含：
        - taskType: 子 Agent 类型
        - description: 任务描述
        - params: 任务参数（JSON 对象，如果参数缺失则为空对象 {}）
        
        如果用户输入中缺少必要参数，params 中对应字段设置为 null。
        
        只返回 JSON 数组，不要包含任何其他文字说明。
        """)
    String analyzeTasks(@V("userMessage") String userMessage, @UserMessage String instruction);

    /**
     * 检查任务参数是否完整，并从用户输入中提取缺失的参数
     * 返回格式: "COMPLETE" 或 "MISSING|缺失的参数名1,缺失的参数名2|友好提示语" 或 "EXTRACT|提取到的参数JSON" 或 "QUERY_TOOL|工具名称"
     */
    @SystemMessage("""
        你是一个智能参数检查和提取 Agent。
        
        任务类型: {{taskType}}
        任务描述: {{description}}
        当前已有参数: {{paramsJson}}
        用户当前输入: {{userMessage}}
        历史对话记录: {{conversationHistory}}
        
        你的任务是：
        1. 首先，从用户的所有历史输入（包括当前输入和历史对话）中尝试提取该任务所需的参数
        2. 然后，结合已有参数和提取到的参数，检查是否完整
        3. 如果参数缺失且用户可能不知道可选值，判断是否需要调用查询工具
        
        ❗重要：你必须从所有对话历史中提取参数，不要只看当前输入！
        
        各任务类型所需参数：
        
        MENU_ORDER 需要参数: 
        - mealType(餐食类型): 早餐/午餐/晚餐/夜宵/下午茶等
        - peopleCount(用餐人数): 数字，如"2人"/"3位"/"4个人"
        
        ROOM_BOOKING 需要参数: 
        - roomType(房型): 标准间/豪华间/豪华套房/总统套房
        - checkInDate(入住日期): 具体日期或相对时间，如"明天"/"2024-12-25"
        - nights(入住天数): 数字
        ✅ 如果缺少 roomType 参数，用户可能不知道有哪些房型，请返回：QUERY_TOOL|queryAvailableRoomTypes
        
        ROOM_RENEWAL 需要参数: 
        - roomNumber(房间号): 房间编号，如"1203"/"2501"
        - renewalDays(续约天数): 数字
        - roomType(房型): 标准间/豪华间等
        
        WAKE_UP_CALL 需要参数: 
        - wakeUpTime(叫醒时间): 时间，如"7点"/"07:00"/"明早八点"
        
        CONCIERGE_SERVICE 需要参数: 
        - serviceType(服务类型): 机场接送/餐厅预订/行李寄存等
        - details(详细需求): 具体要求
        
        提取示例：
        - 用户在历史中说"我要点晚餐"，后来说"3人" → 提取到 mealType="晚餐", peopleCount=3
        - 用户先说"我想预订房间"，后来说"豪华套房"，再说"明天入住2晚" → 提取到 roomType="豪华套房", checkInDate="明天", nights=2
        - 用户先说"我想预订房间"，系统显示房型列表，用户说"标准间" → 提取到 roomType="标准间"
        
        返回格式：
        1. 如果提取到了缺失的参数，返回：EXTRACT|{"mealType":"晚餐","peopleCount":3}
        2. 如果提取后参数完整，返回：COMPLETE
        3. 如果需要调用工具查询可选值，返回：QUERY_TOOL|工具名称
        4. 如果仍然缺少参数（且不需调用工具），返回：MISSING|缺失的参数名|友好的提示语
        
        注意：
        - 优先尝试从用户所有历史输入中提取参数
        - 只返回上述四种格式之一
        - EXTRACT 返回的 JSON 包含所有提取到的参数（包括从历史和当前输入提取的）
        - QUERY_TOOL 用于查询可选值列表，目前支持：queryAvailableRoomTypes
        - 提取参数时要与任务类型所需的参数名称严格对应
        """)
    String checkTaskParams(
        @V("taskType") String taskType,
        @V("description") String description,
        @V("paramsJson") String paramsJson,
        @V("userMessage") String userMessage,
        @V("conversationHistory") String conversationHistory,
        @UserMessage String instruction
    );

    /**
     * 判断是否需要继续执行下一个任务
     * 返回: "CONTINUE" 或 "WAIT_USER_INPUT|提示信息"
     */
    @SystemMessage("""
        你是一个任务流程控制 Agent。
        
        当前已完成的任务结果：
        {{completedTasks}}
        
        待执行的下一个任务：
        任务类型: {{nextTaskType}}
        任务描述: {{nextTaskDescription}}
        
        请判断是否应该继续执行下一个任务：
        
        1. 如果可以直接执行（参数齐全），返回：CONTINUE
        2. 如果需要等待用户进一步输入信息，返回：WAIT_USER_INPUT|友好的提示语
        
        返回格式只能是上述两种之一。
        """)
    String shouldContinueToNextTask(
        @V("completedTasks") String completedTasks,
        @V("nextTaskType") String nextTaskType,
        @V("nextTaskDescription") String nextTaskDescription,
        @UserMessage String instruction
    );
}
