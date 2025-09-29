package com.unicorn.journey.assistant.entity.mappers;

import com.unicorn.journey.assistant.controller.vo.PlanVO;
import com.unicorn.journey.assistant.entity.Plan;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PlanMapper {

    PlanMapper INSTANCE = Mappers.getMapper(PlanMapper.class);

    PlanVO convertToPlanVO(Plan plan);
}
