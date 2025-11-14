# 酒店智能助手系统

## 概述

这是一个基于多 Agent 协作的酒店智能助手系统，使用 LangGraph4j 工作流框架实现，支持 SSE 实时流式输出和人在环路(Human-in-the-Loop)交互。

## 核心功能

1. **智能任务分析**：根据用户输入自动拆分成多个子任务
2. **多 Agent 协作**：不同类型的服务由专门的 Agent 处理
3. **参数智能检查**：自动检测缺失参数并提示用户补充
4. **菜单确认机制**：点餐服务支持用户确认、拒绝或重新生成
5. **流式输出**：使用 SSE 实时返回 AI 生成的内容
6. **工作流暂停/恢复**：等待用户输入时自动暂停，收到输入后继续执行

## 架构设计

### 工作流程图

```
START 
  → resume_router (恢复路由)
  → analyze_tasks (任务分析) 
  → check_params (参数检查)
  → [分支]
      → MENU_ORDER: menu_order (生成菜单) → confirm_menu (确认菜单)
      → OTHER: task_executor (执行任务)
  → next_task_router (下一任务路由)
  → summary (汇总)
  → END
```

### 核心组件

#### 1. Agent 层

- **HotelRouterAgent**: 路由调度 Agent，负责任务分析和参数检查
- **MenuOrderAgent**: 点餐服务 Agent
- **RoomBookingAgent**: 客房预订 Agent
- **WakeUpCallAgent**: 叫醒服务 Agent
- **ConciergeServiceAgent**: 礼宾服务 Agent
- **SummaryAgent**: 汇总 Agent

#### 2. 节点层

- **RouterNode**: 任务分析和参数检查节点
- **MenuOrderNode**: 点餐服务节点
- **TaskExecutorNode**: 其他任务执行节点
- **ConfirmMenuNode**: 菜单确认节点
- **SummaryNode**: 汇总节点

#### 3. 状态管理

- **HotelAssistantContext**: 工作流上下文，存储任务列表、执行结果等

#### 4. 服务层

- **HotelWorkflowService**: 工作流执行服务
- **HotelAgentFactory**: Agent 工厂，管理 Agent 实例

#### 5. 控制器

- **HotelAssistantController**: REST API 控制器

## API 接口

### 1. 启动助手

**接口**: `POST /hotel-assistant/start`

**请求体**:
```json
{
  "userId": "user123",
  "userMessage": "我要点晚餐给2个人，明天早上7点叫我起床"
}
```

**响应**: SSE 流式事件

### 2. 用户确认/输入

**接口**: `POST /hotel-assistant/confirm/{sessionId}`

**请求体（确认菜单）**:
```json
{
  "action": "approved",
  "confirmType": "MENU"
}
```

**请求体（重新生成）**:
```json
{
  "action": "regenerate",
  "confirmType": "MENU"
}
```

**请求体（补充参数）**:
```json
{
  "action": "approved",
  "params": {
    "mealType": "晚餐",
    "peopleCount": 2
  }
}
```

## SSE 事件类型

| 事件类型 | 说明 |
|---------|------|
| workflow_start | 工作流启动 |
| step_update | 步骤更新 |
| output_chunk | 流式输出内容 |
| confirmation_required | 需要用户确认 |
| input_params | 需要用户输入参数 |
| workflow_complete | 工作流完成 |
| workflow_error | 工作流错误 |

## 使用示例

### 示例 1：点餐服务

**用户输入**: "我要点2人份的晚餐，不吃海鲜"

**工作流执行**:
1. 分析任务 → 识别为 MENU_ORDER 任务
2. 检查参数 → mealType=晚餐, peopleCount=2, dietaryRestrictions=不吃海鲜
3. 生成菜单 → AI 生成适合2人的晚餐菜单（无海鲜）
4. 等待确认 → 用户可以确认、拒绝或要求重新生成
5. 继续执行 → 如果用户确认，标记任务完成
6. 生成汇总 → AI 生成服务汇总报告

### 示例 2：多任务组合

**用户输入**: "帮我预订一间豪华套房，明天入住2晚，另外明早7点叫醒我"

**工作流执行**:
1. 分析任务 → 识别为 ROOM_BOOKING + WAKE_UP_CALL
2. 执行第一个任务：
   - 检查参数 → 缺少 checkInDate
   - 提示用户 → "请提供入住日期"
   - 等待输入 → 用户补充日期
   - 执行预订 → AI 生成客房预订确认
3. 执行第二个任务：
   - 检查参数 → wakeUpTime=07:00 完整
   - 执行叫醒 → AI 生成叫醒服务确认
4. 生成汇总 → AI 汇总所有服务内容

## 扩展说明

### 添加新的子 Agent

1. 在 `agent` 包下创建新的 Agent 接口
2. 在 `HotelAgentFactory` 中添加创建方法
3. 在 `TaskExecutorNode` 或创建新节点处理该类型任务
4. 在 `HotelRouterAgent.analyzeTasks` 的 SystemMessage 中添加新的任务类型说明

### 自定义 Agent 行为

修改各个 Agent 接口的 `@SystemMessage` 注解内容，调整 AI 的行为和输出格式。

## 技术栈

- **LangGraph4j**: 工作流编排框架
- **LangChain4j**: AI Agent 框架
- **Spring Boot**: Web 框架
- **SSE**: 服务器推送事件
- **Virtual Thread**: Java 21 虚拟线程

## 注意事项

1. 确保大模型配置正确（ChatModel）
2. 工作流暂停时状态会保存在内存中，重启服务会丢失
3. SSE 连接超时时间为 60 分钟
4. Agent 实例会缓存在内存中，会话结束时需要清理
