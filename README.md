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
 ./mvnw spring-boot:run
 ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments='-Dspring.devtools.restart.enabled=true'
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
#### Woody 聊天
```shell
curl --location 'http://localhost:8080/journey-assistant/woody-chat?userMessage=%E4%BD%A0%E5%A5%BD'
```

#### Duffy 文字聊天
```shell
curl --location 'http://localhost:8080/journey-assistant/duffy-text?userMessage=%E4%BD%A0%E5%A5%BD'
```

### 开启新会话，根据当前用户和助手清空记忆
```shell
curl --location --request PUT 'http://localhost:8080/journey-assistant/new-conversation'
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
## 3. 订单接口
#### 保存订单
```java
/**
 * userId : 下单人的userId
 * purchasedProducts : 订单中包含的产品列表数组, 包含产品ID和产品数量
 * visitDate : 入园日期
 */
```
```shell
curl --location 'http://localhost:8080/journey-assistant/order/create' \
--header 'Content-Type: application/json' \
--data '{
    "userId": 2,
    "purchasedProducts": [
        {
            "productId": 1,
            "quantity": 1
        },
        {
            "productId": 2,
            "quantity": 2
        }
    ],
    "visitDate": "2026-05-04"
}'
```
#### 根据订单号获取订单详情
注意需要根据UUID获取
```shell
curl --location 'http://localhost:8080/journey-assistant/order/detail/ab98c607-b5fc-4c60-9eac-befbc77bdc95'
```

#### 订单退款
通过订单ID对订单进行退款的工具
```java
/**
 * orderId : 订单ID
 * refundPercentage : 退款百分比(0-100)，系统会自动计算退款金额并设置退款类型
 *                    - refundPercentage = 100: 全额退款
 *                    - 0 < refundPercentage < 100: 折扣退款
 */
```
```shell
curl --location --request POST 'http://localhost:8080/journey-assistant/order/refund' \
--header 'Content-Type: application/json' \
--data '{
    "orderId": "ab98c607-b5fc-4c60-9eac-befbc77bdc95",
    "refundPercentage": 50
}'
```

#### 根据用户ID获取订单列表
返回的是一个 list
```shell
curl --location 'http://localhost:8080/journey-assistant/order/list?userId=1'
```

#### 根据订单ID获取退款日志
返回指定订单的退款日志列表
```shell
curl --location 'http://localhost:8080/journey-assistant/refund/log/ab98c607-b5fc-4c60-9eac-befbc77bdc95'
```

#### 获取所有退款日志
返回系统中所有的退款日志列表
```shell
curl --location 'http://localhost:8080/journey-assistant/refund/log/all'
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
