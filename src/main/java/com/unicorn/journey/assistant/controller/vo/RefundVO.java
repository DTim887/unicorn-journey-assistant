package com.unicorn.journey.assistant.controller.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 退款信息视图对象
 */
@Getter
@Setter
@NoArgsConstructor
public class RefundVO {

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("totalPrice")
    private int totalPrice;

    @JsonProperty("refundPrice")
    private int refundPrice;

    @JsonProperty("refundTime")
    private LocalDateTime refundTime;

    @JsonProperty("status")
    private String status;

    @JsonProperty("type")
    private String type;
}