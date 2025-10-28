package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.chat.AiService;
import com.unicorn.journey.assistant.chat.AiServiceFactory;
import com.unicorn.journey.assistant.controller.vo.RedNoteListVO;
import com.unicorn.journey.assistant.controller.vo.RedNoteVO;
import com.unicorn.journey.assistant.controller.vo.Result;
import com.unicorn.journey.assistant.entity.RedNote;
import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.entity.mappers.RedNoteMapper;
import com.unicorn.journey.assistant.service.RedNoteService;
import com.unicorn.journey.assistant.service.UserService;
import com.unicorn.journey.assistant.utils.RelativeTimeConverter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Comparator;
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
        String userMessage = "帮我查询小红书的笔记，关键字是\"上海迪士尼\", 并分析并总结有舆情风险的笔记";
//        String userMessage = "你好";
        log.info("Send text:{}, memoryId:{} ", userMessage, memoryId);
        return aiService.xiaoHongShu(memoryId, userMessage);
    }


    @GetMapping("/xiaohongshu/all")
    public Result getAll() {
        List<RedNote> redNoteList = redNoteService.getAllRedNote();
        List<RedNoteVO> redNoteVOs = redNoteList.stream().map(RedNoteMapper.INSTANCE::convertToRedNoteVO).toList();
        //根据日期排序
        redNoteVOs = redNoteVOs.stream()
                .sorted(Comparator.comparing(RedNoteVO::getCreateDateTime,Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        //日期转换成分钟
        for (RedNoteVO redNoteVO : redNoteVOs) {
            redNoteVO.setCreateDateTime(RelativeTimeConverter.convert(redNoteVO.getCreateDateTime()));
        }
        log.info("总共查到:{} 篇笔记", redNoteList.size());
        RedNoteListVO redNoteListVO = RedNoteListVO.builder()
                .redNoteList(redNoteVOs)
                .build();
        redNoteListVO.calculateTagCounts();
        redNoteListVO.calculateRiskLevelCounts();
        return Result.ok(redNoteListVO);
    }
}
