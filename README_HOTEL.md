# 酒店智能助手接口文档

## 概述

酒店智能助手是一个基于AI的智能服务系统，为酒店客人提供订餐、叫醒服务等便捷功能。系统支持文本和语音两种交互方式，通过SSE（Server-Sent Events）实现实时流式响应。

**基础路径**: `/journey-assistant/hotel`

---

## 接口列表

### 1. 聊天接口

#### 1.1 文本聊天

**接口地址**: `POST /hotel/chat`

**接口说明**: 与酒店AI助手进行文本对话，支持订餐、叫醒服务等功能。通过SSE流式返回响应。

**Content-Type**: `application/json`

**响应类型**: `text/event-stream` (SSE)

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | String | 是 | 用户ID |
| message | String | 是 | 用户消息内容 |
| sessionId | String | 否 | 会话ID，首次对话可为空，后续对话需携带 |
| enableVoiceOutput | Boolean | 否 | 是否启用语音输出，默认为 false |
| voiceCharacter | String | 否 | 语音角色选择，可选值："NICK"（尼克）或 "JUDY"（朱迪），默认为 "NICK" |

**语音角色说明**:
- **NICK（尼克）**: 男性语音，声音沉稳、专业
- **JUDY（朱迪）**: 女性语音，声音亲切、温柔
- 前端可根据用户偏好选择不同的语音角色，提供个性化服务体验

**请求示例**:

```bash
curl --location 'http://localhost:8080/journey-assistant/hotel/chat' \
--header 'Content-Type: application/json' \
--data '{
    "userId": "1",
    "message": "我想订餐",
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "enableVoiceOutput": true,
    "voiceCharacter": "JUDY"
}'
```

**使用尼克语音的示例**:

```bash
curl --location 'http://localhost:8080/journey-assistant/hotel/chat' \
--header 'Content-Type: application/json' \
--data '{
    "userId": "1",
    "message": "帮我设置明天早上7点的叫醒服务",
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "enableVoiceOutput": true,
    "voiceCharacter": "NICK"
}'
```


---

#### 1.2 语音聊天

**接口地址**: `POST /hotel/voice-chat`

**接口说明**: 通过语音与酒店AI助手对话。系统会自动将语音转换为文字，并返回语音回复。

**Content-Type**: `multipart/form-data`

**响应类型**: `text/event-stream` (SSE)

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| audio | File | 是 | 语音文件（支持 WAV 格式） |
| userId | String | 是 | 用户ID |
| sessionId | String | 否 | 会话ID，首次对话可为空 |
| enableVoiceOutput | Boolean | 否 | 是否启用语音输出，默认为 true |
| voiceCharacter | String | 否 | 语音角色选择，可选值："NICK"（尼克）或 "JUDY"（朱迪），默认为 "NICK" |

**请求示例**:

```bash
curl -X POST \
  -F "audio=@/path/to/audio.wav" \
  -F "userId=1" \
  -F "sessionId=550e8400-e29b-41d4-a716-446655440000" \
  -F "enableVoiceOutput=true" \
  -F "voiceCharacter=JUDY" \
  http://localhost:8080/journey-assistant/hotel/voice-chat
```

**参数说明**:

- `enableVoiceOutput`: 当设置为 `false` 时，系统只返回文字回复，不生成语音文件。这样可以减少服务器资源消耗。默认为 `true`，使用语音聊天时自动生成语音回复。
---

### 2. 叫醒服务接口

#### 2.1 获取用户叫醒服务列表

**接口地址**: `GET /hotel/wakeup-assistance/{userId}`

**接口说明**: 获取指定用户的所有叫醒服务记录

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | String | 是 | 用户ID（路径参数） |

**响应示例**:

```json
[
  {
    "wakeUpId": "wakeup_1234567890",
    "userId": "1",
    "wakeUpTime": "2024-11-19T07:30:00",
    "createTime": "2024-11-18T22:00:00",
    "status": "PENDING",
    "remark": "请准时叫醒"
  }
]
```

**状态说明**:
- `PENDING`: 待执行
- `ACTIVE`: 已激活
- `COMPLETED`: 已完成
- `CANCELLED`: 已取消

**请求示例**:

```bash
curl --location 'http://localhost:8080/journey-assistant/hotel/wakeup-assistance/1'
```

---

#### 2.2 获取叫醒服务详情

**接口地址**: `GET /hotel/wakeup-service/{wakeUpId}`

**接口说明**: 根据叫醒服务ID获取详细信息

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| wakeUpId | String | 是 | 叫醒服务ID（路径参数） |

**响应示例**: 同上叫醒服务对象

**请求示例**:

```bash
curl --location 'http://localhost:8080/journey-assistant/hotel/wakeup-service/wakeup_1234567890'
```

---

#### 2.3 删除叫醒服务

**接口地址**: `DELETE /hotel/wakeup-service/{wakeUpId}`

**接口说明**: 删除指定的叫醒服务，同时取消定时任务

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| wakeUpId | String | 是 | 叫醒服务ID（路径参数） |

**响应示例**:

```json
{
  "success": true,
  "wakeUpId": "WAKEUP_1234567890",
  "message": "叫醒服务已删除"
}
```

**失败响应**:

```json
{
  "success": false,
  "wakeUpId": "WAKEUP_1234567890",
  "message": "叫醒服务不存在"
}
```

**请求示例**:

```bash
curl -X DELETE \
  http://localhost:8080/journey-assistant/hotel/wakeup-service/WAKEUP_1234567890
```

---

### 3. 语音服务接口

#### 3.1 语音转文字

**接口地址**: `POST /hotel/speech-to-text`

**接口说明**: 将语音文件转换为文字文本。这是一个独立的API，可供前端单独调用，无需进入聊天流程。

**Content-Type**: `multipart/form-data`

**响应类型**: `application/json`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| audio | File | 是 | 语音文件（支持 WAV 格式） |

**成功响应示例**:

```json
{
  "code": "0",
  "msg": "success",
  "data": {
    "text": "我想订一份清蒸鲈鱼",
    "fileName": "audio_20241119_103045.wav",
    "fileSize": 125680
  }
}
```

**失败响应示例**:

```json
{
  "code": "500",
  "msg": "语音识别失败",
  "data": {
    "error": "Audio format not supported"
  }
}
```

**请求示例**:

```bash
curl -X POST \
  -F "audio=@/path/to/audio.wav" \
  http://localhost:8080/journey-assistant/hotel/speech-to-text
```

**使用场景**:

1. **独立语音转文字**: 前端可以单独调用此接口将用户的语音转换为文字，然后显示在输入框中
2. **语音输入辅助**: 用户录音后先转文字预览，确认无误后再发送
3. **语音笔记**: 将语音内容转换为文字备忘录

**与语音聊天接口的区别**:

| 对比项 | speech-to-text | voice-chat |
|--------|----------------|------------|
| 返回类型 | JSON | SSE流 |
| 功能 | 仅转文字 | 转文字 + AI对话 + 语音回复 |
| 会话管理 | 无需会话 | 需要 sessionId |
| 使用场景 | 单纯转录 | 完整对话流程 |

---

### 4. 会话管理接口

#### 4.1 清除会话

**接口地址**: `DELETE /hotel/session/{sessionId}`

**接口说明**: 清除指定会话的所有数据，包括对话历史和SSE连接

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | String | 是 | 会话ID（路径参数） |

**请求示例**:

```bash
curl -X DELETE \
  http://localhost:8080/journey-assistant/hotel/session/550e8400-e29b-41d4-a716-446655440000
```

---

## SSE 事件协议文档

### SSE 协议说明

酒店智能助手使用 SSE（Server-Sent Events）协议进行实时通信。所有聊天接口（`/chat` 和 `/voice-chat`）都通过 SSE 流式返回响应。

### SSE 事件格式

SSE 消息格式遵循以下规范：

```
event: <事件类型>
data: <JSON数据>

```

**注意**: 每条消息以双换行符（`\n\n`）分隔

---

### SSE 事件类型

#### 1. session_created - 会话创建

**触发时机**: 首次对话时，sessionId 为空

**数据格式**:

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**说明**: 前端收到此事件后，需保存 sessionId 用于后续对话

**SSE 消息示例**:

```
event: session_created
data: {"sessionId":"550e8400-e29b-41d4-a716-446655440000"}

```

---

#### 2. message - 普通消息

**触发时机**: AI 返回文本消息时

**数据格式**:

```json
{
  "content": "欢迎来到上海迪士尼乐园酒店！今天我能为您提供订餐或叫醒服务吗？"
}
```

**SSE 消息示例**:

```
event: message
data: {"content":"欢迎来到上海迪士尼乐园酒店！今天我能为您提供订餐或叫醒服务吗？"}

```

---

#### 3. structured_data - 结构化数据

**触发时机**: 返回菜单、订单、叫醒服务等结构化数据时

**数据格式**:

```json
{
  "type": "MENU|CONFIRM|ORDER|TIME_INPUT|WAKE_UP",
  "data": { /* 具体数据对象 */ }
}
```

**类型说明**:

- **MENU**: 菜单列表（初始菜单或筛选后菜单）
  
  ```json
  {
    "type": "MENU",
    "data": [
      {
        "menuId": 1,
        "name": "清蒸鲈鱼",
        "category": "中式",
        "flavors": ["清淡", "鲜"],
        "image": "/images/menu/steamed_sea_bass.jpg",
        "price": 68.0,
        "description": "新鲜鲈鱼清蒸，保留原味"
      },
      {
        "menuId": 2,
        "name": "剁椒鱼头",
        "category": "中式",
        "flavors": ["辣", "鲜"],
        "image": "/images/menu/fish_head.jpg",
        "price": 68.0,
        "description": "剁椒鱼头，麻辣鲜香"
      },
      {
        "menuId": 7,
        "name": "麻婆豆腐",
        "category": "中式",
        "flavors": ["辣", "麻"],
        "image": "/images/menu/mapo_tofu.jpg",
        "price": 32.0,
        "description": "传统川菜，麻辣豆腐"
    ]
  }
  ```

- **CONFIRM**: 确认菜单（用户已选择菜品，等待最终确认）
  
  ```json
  {
    "type": "CONFIRM",
    "data": [
      {
        "menuId": 1,
        "name": "清蒸鲈鱼",
        "category": "中式",
        "flavors": ["清淡", "鲜"],
        "image": "/images/menu/steamed_sea_bass.jpg",
        "price": 68.0,
        "description": "新鲜鲈鱼清蒸，保留原味"
      },
      {
        "menuId": 7,
        "name": "麻婆豆腐",
        "category": "中式",
        "flavors": ["辣", "麻"],
        "image": "/images/menu/mapo_tofu.jpg",
        "price": 32.0,
        "description": "传统川菜，麻辣豆腐"
      }
    ]
  }
  ```

- **ORDER**: 订单数据（订单已确认，待执行）
  
  ```json
  {
    "type": "ORDER",
    "data": {
      "orderId": "ORDER_1234567890",
      "userId": "1",
      "items": [
        {
          "menuId": 1,
          "name": "清蒸鲈鱼",
          "category": "中式",
          "price": 68.0
        },
        {
          "menuId": 7,
          "name": "麻婆豆腐",
          "category": "中式",
          "price": 32.0
        }
      ],
      "totalPrice": 100.0,
      "createTime": "2024-11-18T10:30:00",
      "status": "CONFIRMED"
    }
  }
  ```

- **TIME_INPUT**: 请求用户输入叫醒时间
  
  ```json
  {
    "type": "TIME_INPUT",
    "data": {
      "message": "请选择叫醒时间"
    }
  }
  ```

- **WAKE_UP**: 叫醒服务数据（叫醒服务已创建）
  
  ```json
  {
    "type": "WAKE_UP",
    "data": {
      "wakeUpId": "WAKEUP_1234567890",
      "userId": "1",
      "sessionId": "session_1_1234567890",
      "wakeUpTime": "2024-11-19T07:30:00",
      "createTime": "2024-11-18T22:00:00",
      "status": "PENDING",
      "voicePath": "/voice/voice_1234567890_abc123.wav",
      "remark": "酒店叫醒服务"
    }
  }
  ```

**SSE 消息示例**:

```
event: structured_data
data: {"type":"MENU","data":[{"menuId":1,"name":"清蒸鲈鱼","price":68.0}]}

```

**structured_data 字段详细说明**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| type | String | 是 | 数据类型（MENU/CONFIRM/ORDER/TIME_INPUT/WAKE_UP） |
| data | Object/Array | 是 | 条件数据内容，不同 type 有不同结构 |

**MenuItem 字段详细说明** （用于 MENU/CONFIRM 中的菜品元素）:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| menuId | Integer | 是 | 菜品ID |
| name | String | 是 | 菜品名称 |
| category | String | 是 | 菜品分类（中式/西式） |
| price | Double | 是 | 菜品价格 |
| flavors | Array | 否 | 口味标签 |
| description | String | 否 | 菜品描述 |
| image | String | 否 | 菜品图片路径 |

**Order 字段详细说明**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderId | String | 是 | 订单ID |
| userId | String | 是 | 用户ID |
| items | Array | 是 | 菜品列表（MenuItem数组） |
| totalPrice | Double | 是 | 总价格 |
| createTime | DateTime | 是 | 订单创建时间 |
| status | String | 是 | 订单状态（CONFIRMED） |

**WakeUpAssistance 字段详细说明**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| wakeUpId | String | 是 | 叫醒服务ID |
| userId | String | 是 | 用户ID |
| sessionId | String | 是 | 会话ID |
| wakeUpTime | DateTime | 是 | 叫醒时间 |
| createTime | DateTime | 是 | 叫醒服务创建时间 |
| status | String | 是 | 服务状态（PENDING/ACTIVE/COMPLETED/CANCELLED） |
| voicePath | String | 否 | 叫醒语音文件路径 |
| remark | String | 否 | 叫醒服务备注 |

---

#### 4. confirm_menu - 确认菜单

**触发时机**: 需要用户确认订单时

**数据格式**:

```json
{
  "data": [
    {
      "menuId": 1,
      "name": "清蒸鲈鱼",
      "category": "中式",
      "flavors": ["清淡", "鲜"],
      "image": "/images/menu/steamed_sea_bass.svg",
      "price": 68.0,
      "description": "新鲜鲈鱼清蒸，保留原味",
      "servings": 2
    }
  ]
}
```

**SSE 消息示例**:

```
event: confirm_menu
data: {"data":[{"menuId":1,"name":"清蒸鲈鱼","price":68.0}]}

```

---

#### 5. voice - 语音消息

**触发时机**: 语音聊天时，返回语音文件路径

**数据格式**:

```json
{
  "audioPath": "/journey-assistant/voice/voice_JUDY_1234567890_abc123.mp3",
  "text": "您好，我已为您推荐了几道菜品",
  "type": "assistant",
  "voiceCharacter": "JUDY"
}
```

**字段说明**:
- `audioPath`: 语音文件访问路径（文件名包含角色信息）
- `text`: 语音对应的文本内容
- `type`: 消息类型标识（assistant 表示助手回复）
- `voiceCharacter`: 语音角色名称（NICK 或 JUDY）

**SSE 消息示例**:

```
event: voice
data: {"audioPath":"/journey-assistant/voice/voice_NICK_1234567890_abc123.mp3","text""您好","type":"assistant","voiceCharacter":"NICK"}

```

---

#### 6. wakeup_alert - 叫醒提醒事件

**触发时机**: 叫醒服务到达设定时间时，后端自动发送

**数据格式**:

```json
{
  "wakeUpId": "WAKEUP_1234567890",
  "wakeUpTime": "2024-11-19T07:30:00",
  "voicePath": "/journey-assistant/voice/voice_1234567890_abc123.wav",
  "message": "您的叫醒时间到了！"
}
```

**字段说明**:
- `wakeUpId`: 叫醒服务ID
- `wakeUpTime`: 叫醒时间
- `voicePath`: 叫醒语音文件路径（用于自动播放）
- `message`: 叫醒提示文字

**前端处理逻辑**:
1. 收到事件后弹出叫醒弹窗
2. 自动播放 `voicePath` 指定的语音文件
3. 显示“稍后提醒”和“我已起床”按钮
4. 用户点击确认后关闭弹窗

**SSE 消息示例**:

```
event: wakeup_alert
data: {"wakeUpId":"WAKEUP_1234567890","wakeUpTime":"2024-11-19T07:30:00","voicePath":"/journey-assistant/voice/voice_1234567890_abc123.wav","message":"您的叫醒时间到了！"}

```

**叫醒语音示例**:

系统会根据叫醒时间生成友好的语音内容：

```
尊敬的客人，早上好！

现在是7点，该起床啦！

祝您今天有个美好的一天！
```

**时间问候语**:
- 5:00-9:00 → “早上好”
- 9:00-12:00 → “上午好”
- 12:00-14:00 → “中午好”
- 14:00-18:00 → “下午好”
- 其他时间 → “晚上好”

---

#### 7. error - 错误消息

**触发时机**: 发生错误时

**数据格式**:

```json
{
  "error": "会话已过期，请重新开始对话",
  "message": "Session expired"
}
```

**SSE 消息示例**:

```
event: error
data: {"error":"会话已过期，请重新开始对话"}

```



## 数据模型

### ChatRequest - 聊天请求

```java
{
  "userId": "String",           // 用户ID（必填）
  "message": "String",          // 消息内容（必填）
  "sessionId": "String",        // 会话ID（选填）
  "enableVoiceOutput": Boolean,  // 是否启用语音输出（选填，默认 false）
  "voiceCharacter": "String"    // 语音角色（选填，可选值："NICK" 或 "JUDY"，默认 "NICK"）
}
```







---

## 常见问题

### Q1: 如何保持会话连接？

A: SSE 连接会保持 60 分钟。在此期间使用相同的 `sessionId` 可以继续对话。超时后后端会自动创建新会话。

### Q2: 语音文件支持哪些格式？

A: 目前支持 WAV 格式的音频文件。

### Q3: 不需要语音回复怎么请求？

A: 文本聊天接口 `/chat` 默认不生成语音。如需要语音回复，请设置 `enableVoiceOutput: true` 参数。

语音聊天接口 `/voice-chat` 默认启用语音输出。如不需要，请设置 `enableVoiceOutput: false` 参数。

### Q4: 如何选择不同的语音角色？

A: 系统提供两种语音角色供选择：

- **NICK（尼克）**: 男性语音，声音沉稳、专业，适合商务场景
- **JUDY（朱迪）**: 女性语音，声音亲切、温柔，适合休闲场景

**使用方式**:

1. **文本聊天接口**: 在请求体中添加 `voiceCharacter` 参数
   ```json
   {
     "userId": "1",
     "message": "我想订餐",
     "enableVoiceOutput": true,
     "voiceCharacter": "JUDY"  // 选择朱迪的语音
   }
   ```

2. **语音聊天接口**: 在 form-data 中添加 `voiceCharacter` 字段
   ```bash
   curl -X POST \
     -F "audio=@/path/to/audio.wav" \
     -F "userId=1" \
     -F "voiceCharacter=NICK" \
     http://localhost:8080/journey-assistant/hotel/voice-chat
   ```

**注意**:
- 如果不指定 `voiceCharacter` 参数，系统默认使用 NICK（尼克）语音
- 语音角色选择同时适用于聊天响应和叫醒服务语音
- 前端可以根据用户偏好保存设置，在每次请求中携带对应的角色参数



