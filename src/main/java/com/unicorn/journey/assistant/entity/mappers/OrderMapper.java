package com.unicorn.journey.assistant.entity.mappers;

import com.unicorn.journey.assistant.controller.request.CreateOrderRequest;
import com.unicorn.journey.assistant.controller.vo.OrderVO;
import com.unicorn.journey.assistant.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "status", ignore = true)
    })
    Order convertToOrder(CreateOrderRequest createOrderRequest);

    @Mappings({
        @Mapping(source = "purchasedProducts", target = "purchasedProductVOs"),
        @Mapping(target = "totalPrice", ignore = true)
    })
    OrderVO convertToOrderVO(Order order);

    @Mappings({
        @Mapping(target = "productName", ignore = true),
        @Mapping(target = "price", ignore = true)
    })
    OrderVO.PurchasedProductVO convertToPurchasedProductVO(Order.PurchasedProduct purchasedProduct);
}
