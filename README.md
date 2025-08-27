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

### APIs
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

#### chat
和 LLM 对话，流式返回
```shell
curl --location 'http://localhost:8080/journey-assistant/stream-chat?userMessage=%E7%99%BB%E5%BD%95%E7%94%A8%E6%88%B7'
```

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

#### 获取所有产品
```shell
curl --location 'http://localhost:8080/journey-assistant/product/all'

```
#### 根据产品名称获取产品
```shell
curl --location 'http://localhost:8080/journey-assistant/product/上海迪士尼乐园纪念品礼包'
```