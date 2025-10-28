package com.unicorn.journey.assistant.service;


import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.entity.RedNote;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@LocalCache(value = CacheName.REDNOTE)
public class RedNoteService extends BaseService<RedNote> {


    // ISO 8601 格式化器
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 保存小红书笔记到缓存
     * @param redNote
     */
    public void saveRedNote(RedNote redNote) {
        this.put(redNote.getId(), redNote);
    }

    /**
     * 批量保存小红书笔记
     * @param redNotes
     */
    @Tool
    public void saveRedNotes(List<RedNote> redNotes) {
        for (RedNote redNote : redNotes) {
            log.info("redNote.createDateTime:{}", redNote.getCreateDateTime());
            redNote.setCreateDateTime(LocalDateTime.now().format(ISO_FORMATTER));
            this.put(redNote.getId(), redNote);
        }
    }

    /**
     * 获取所有小红书笔记
     * @return
     */
    public List<RedNote> getAllRedNote() {
        return this.getAll(RedNote.class);
    }
}
