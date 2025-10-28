package com.unicorn.journey.assistant.entity.mappers;

import com.unicorn.journey.assistant.controller.vo.RedNoteVO;
import com.unicorn.journey.assistant.entity.RedNote;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface RedNoteMapper {

    RedNoteMapper INSTANCE = Mappers.getMapper(RedNoteMapper.class);

    RedNoteVO convertToRedNoteVO(RedNote redNote);
}
