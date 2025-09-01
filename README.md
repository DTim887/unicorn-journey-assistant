# unicorn-journey-assistant
An agent for journey connects with lazy commerce

## How to start
Clone the repository and locate to the project directory 
### (Linux/macOS)
```terminaloutput
./mvnw clean package
```
### (Windows)
```terminaloutput
.mvnw.cmd clean package
```

### Run JAR file
```terminaloutput
 java -jar target/unicorn-journey-assistant-1.0.0.jar 
```
Open the bowser and visit : http://localhost:8088/journey-assistant

## APIs
### 1. 聊天接口
#### 输入语音 POST 接口, 只有 duffy 才能接收语音, 系统会转换语音为文字并且输出给大模型
前端代码已经提交到 https://github.com/DTim887/myplan-ui 可下载参考
```shell
curl -v -X POST \
  -F "audio=@/path/to/your/local/audio_file.wav" \
  http://localhost:8080/journey-assistant/duffy-chat
````

#### Judy 警官的聊天接口
和 LLM 对话，流式返回
```shell
curl --location 'http://localhost:8080/journey-assistant/judy-chat?userMessage=%E7%99%BB%E5%BD%95%E7%94%A8%E6%88%B7'
```

### 2. 用户接口
#### 用户登录
切换用户登录后 Memory 将随之切换，LLM 会知道当前登录人是谁
```shell
curl --location 'http://localhost:8080/journey-assistant/user/login' \
--header 'Content-Type: application/json' \
--data '{
    "nickname":"Seval"
}'
```
#### 查看当前用户
```shell
curl --location 'http://localhost:8080/journey-assistant/user/current'
```
#### 查看所有用户
```shell
curl --location 'http://localhost:8080/journey-assistant/user/all'
```
### 3. 订单接口
#### 保存订单
```shell
curl --location 'http://localhost:8080/journey-assistant/order/save' \
--header 'Content-Type: application/json' \
--data '{
    "id":2,
    "userId":2,
    "productName":"二日票",
    "price":300.00
}'
```

#### 根据订单号获取订单
```shell
curl --location 'http://localhost:8080/journey-assistant/order/get/2'
```

#### 根据用户ID获取订单
```shell
curl --location 'http://localhost:8080/journey-assistant/order/get?userId=2'
```
### 4. 产品接口
#### 获取所有产品
```shell
curl --location 'http://localhost:8080/journey-assistant/product/all'
```
#### 根据产品id获取产品
```shell
curl --location 'http://localhost:8080/journey-assistant/product/1'
```

### 5. 景点接口
#### 获取所有景点信息
```shell
curl --location 'http://localhost:8080/journey-assistant/attraction/all'
```

### 6. 行程接口
#### 创建行程
```shell
curl --location --request POST 'localhost:8080/journey-assistant/plan/create' \
--header 'Content-Type: application/json' \
--data-raw '{
    "id": 1,
    "planName" : "一日游",
    "planDate" : "2025-08-29",
    "attractionIds": [1,2,3],
    "userId":1
}'
```

#### 根据用户ID获取用户的所有plan信息
```shell
curl --location --request GET 'localhost:8080/journey-assistant/plan/get?userId=1'
```

#### 根据用户ID获取用户的所有plan信息
```shell
curl --location --request GET 'localhost:8080/journey-assistant/plan/get/1'
```

### 7. 助手接口
#### 当前服务的助手
```shell
curl --location 'http://localhost:8080/journey-assistant/assistant/current'
```
#### 切换助手
```shell
curl --location 'http://localhost:8080/journey-assistant/assistant/exchange' \
--header 'Content-Type: application/json' \
--data '{
    "assistantName":"朱迪"
}'
```
#### 所有助手
```shell
curl --location 'http://localhost:8080/journey-assistant/assistant/all'
```

#### 助手介绍
**朱迪** : 兔子警官，迪士尼向导，帮助用户创建行程，根据行程快速创建订单。
**达菲** : 可爱小熊，倾听者，只接收语音。用户想买啥，语音告诉达菲即可。
#### 助手规则
助手+用户id 作为 memory id, 记忆隔离, system prompt 隔离
