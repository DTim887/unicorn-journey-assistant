package com.unicorn.journey.assistant.entity;

import dev.langchain4j.model.output.structured.Description;
import lombok.*;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Description("用户id")
    private int id;

    @Description("用户名")
    private String nickname;

    @Description("用户性别")
    private String sex;

    @Description("用户头像，图片文件路径")
    private String avatar;

    @Description("用户标签")
    private String[] tags;
}
