package com.unicorn.journey.assistant.entity;

import dev.langchain4j.model.output.structured.Description;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 4L;

    @Description("用户的ID，字段类型是整形，示例值{\"id\": 2}")
    private int id;

    @Description("用户的昵称，字段类型是字符串，示例值{\"nickname\": \"Jessica\"}")
    private String nickname;

    @Description("用户的性别，字段类型是字符串，示例值{\"sex\": \"女\"}")
    private String sex;

    @Description("用户的头像图片的相对路径，字段类型是字符串，示例值{\"avatar\": \"/images/panda.png\"}")
    private String avatar;

    @Description("用户的个性化标签，字段类型是字符串数组，示例值{\"tags\": [\"可爱\",\"浪漫\",\"冒险\"]}")
    private String[] tags;
}
