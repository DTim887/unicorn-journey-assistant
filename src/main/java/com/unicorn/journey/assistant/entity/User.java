package com.unicorn.journey.assistant.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class User {
    private int id;
    private String nickname;
}
