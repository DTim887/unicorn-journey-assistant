package com.unicorn.journey.assistant.entity;

import dev.langchain4j.model.output.structured.Description;
import lombok.*;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Assistant {

    @Description("助手id")
    private int id;

    @Description("助手名称")
    private String assistantName;

    @Description("助手头像，图片的文件地址")
    private String avatar;

    @Description("使用此助手调用的API接口")
    private String chatApi;

    @Description("助手描述")
    private String description;
}
