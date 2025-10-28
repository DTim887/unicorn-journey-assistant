package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.chat.AiService;
import com.unicorn.journey.assistant.chat.AiServiceFactory;
import com.unicorn.journey.assistant.controller.vo.RedNoteListVO;
import com.unicorn.journey.assistant.controller.vo.Result;
import com.unicorn.journey.assistant.entity.RedNote;
import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.service.RedNoteService;
import com.unicorn.journey.assistant.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
public class XiaoHongShuController {

    private static final String XIAOHONGSHU = "XIAOHONGSHU_";

    @Resource
    private UserService userService;
    @Resource
    private AiServiceFactory aiServiceFactory;
    @Resource
    private RedNoteService redNoteService;

    @GetMapping("/xiaohongshu")
    public Flux<String> xiaoHongShu() {
        User user = userService.currentUser();
        String memoryId = XIAOHONGSHU + user.getId();
        //Remembering the current logged-in user
        AiService aiService = aiServiceFactory.getXiaoHongShuAiService();
        String userMessage = "帮我查询小红书的笔记，关键字是\"迪士尼\", 并分析并总结有舆情风险的笔记";
//        String userMessage = "你好";
        log.info("Send text:{}, memoryId:{} ", userMessage, memoryId);
        return aiService.xiaoHongShu(memoryId, userMessage);
    }


    @GetMapping("/xiaohongshu/all")
    public Result getAll() {
        List<RedNote> redNoteList = redNoteService.getAllRedNote();
        log.info("总共查到:{} 篇笔记", redNoteList.size());
        RedNoteListVO redNoteListVO = RedNoteListVO.builder()
                .redNoteList(redNoteList)
                .build();
        redNoteListVO.calculateTagCounts();
        redNoteListVO.calculateRiskLevelCounts();
        return Result.ok(redNoteListVO);
    }
}
