package com.unicorn.journey.assistant.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // 序列化时忽略 null 值
public class AudioRequest {

    // Getters and Setters
    @JsonProperty("user")
    private User user;

    @JsonProperty("audio")
    private Audio audio;

    @JsonProperty("request")
    private Request request;

    // 默认构造函数
    public AudioRequest() {
    }

    // 带参数的构造函数
    public AudioRequest(User user, Audio audio, Request request) {
        this.user = user;
        this.audio = audio;
        this.request = request;
    }

    // User 子对象类
    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static
    class User {
        @JsonProperty("uid")
        private String uid;

        public User() {
        }

        public User(String uid) {
            this.uid = uid;
        }
    }

    // Audio 子对象类
    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static
    class Audio {
        @JsonProperty("data")
        private String data; // 存储 Base64 编码的音频数据

        public Audio(String data) {
            this.data = data;
        }

        public Audio() {
        }


    }

    // Request 子对象类
    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static
    class Request {
        @JsonProperty("model_name")
        private String modelName;

        public Request() {
        }

        public Request(String modelName) {
            this.modelName = modelName;
        }
    }
}
