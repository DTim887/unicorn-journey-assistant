package com.unicorn.journey.assistant.entity.mappers;

import com.unicorn.journey.assistant.controller.request.CreateOrderRequest;
import com.unicorn.journey.assistant.controller.vo.OrderVO;
import com.unicorn.journey.assistant.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface OrderMapper {

    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

    Order convertToOrder(CreateOrderRequest createOrderRequest);

    OrderVO convertToOrderVO(Order order);
}
