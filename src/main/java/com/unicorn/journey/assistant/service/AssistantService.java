package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.entity.Assistant;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@LocalCache(value = CacheName.ASSISTANT)
public class AssistantService extends BaseService<Assistant> {

    private static final ConcurrentMap<String, Assistant> assistantCache = new  ConcurrentHashMap<>();


    public void saveAssistant(Assistant assistant) {
        this.put(assistant.getAssistantName(), assistant);
    }

    public Assistant retrieveAssistantByName(String assistantName) {
        return this.get(assistantName);
    }

    public List<Assistant> retrieveAllAssistants() {
        return this.getAll(Assistant.class);
    }

    public void exchange(Assistant assistant) {
        assistantCache.put("current", assistant);
    }

    //获取当前登录用户
    public Assistant currentAssistant() {
        return assistantCache.get("current");
    }

}
