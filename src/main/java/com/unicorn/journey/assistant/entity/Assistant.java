package com.unicorn.journey.assistant.entity;

import lombok.*;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Assistant {

    private int id;
    private String assistantName;
    private String avatar;
    private String chatApi;
    private String description;
}
