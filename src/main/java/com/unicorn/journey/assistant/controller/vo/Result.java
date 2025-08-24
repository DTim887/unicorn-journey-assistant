package com.unicorn.journey.assistant.controller.vo;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Result {


    public static final String OK_CODE = "0";
    public static final String OK_MSG = "success";


    private String code;
    private String msg;
    private Object data;


    public static Result ok(){
        return Result.builder().code(OK_CODE).msg(OK_MSG).build();
    }

    public static  Result ok(Object data){
        return Result.builder().code(OK_CODE).msg(OK_MSG).data(data).build();
    }
}
