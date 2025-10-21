package com.unicorn.journey.assistant.service;


import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.entity.RedNote;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@LocalCache(value = CacheName.REDNOTE)
public class RedNoteService extends BaseService<RedNote> {

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
