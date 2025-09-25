package com.unicorn.journey.assistant.entity.mappers;

import com.unicorn.journey.assistant.controller.vo.ProductVO;
import com.unicorn.journey.assistant.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ProductMapper {

    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);

    ProductVO convertToProductVO(Product product);
}
