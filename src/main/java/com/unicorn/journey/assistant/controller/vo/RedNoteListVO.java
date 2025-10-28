package com.unicorn.journey.assistant.controller.vo;

import com.unicorn.journey.assistant.entity.RedNote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@Builder
public class RedNoteListVO {

    private List<RedNote> redNoteList;

    private TagCount[] tagCounts;

    private RiskLevelCount[] riskLevelCounts;


    public void calculateTagCounts() {
        Stream<String> stream = Stream.empty();
        for (int i = 0; i < this.redNoteList.size(); i++) {
            stream = Stream.concat(stream, Arrays.stream(redNoteList.get(i).getRiskTags()));
        }
        // 过滤掉null值，然后使用Stream进行分组统计
        Map<String, Long> labelMap = stream
                .filter(Objects::nonNull)  // 过滤掉null值
                .collect(Collectors.groupingBy(
                        name -> name,
                        Collectors.counting()
                ));
        this.tagCounts = labelMap.entrySet().stream()
                .map(entry -> new TagCount(entry.getKey(), entry.getValue()))
                .toArray(TagCount[]::new);
    }

    public void calculateRiskLevelCounts() {

        Map<Integer, Long> labelMap = this.redNoteList.stream()
                .filter(Objects::nonNull)  // 过滤掉null值
                .collect(Collectors.groupingBy(
                        RedNote::getRiskLevel,
                        Collectors.counting()
                ));
        this.riskLevelCounts = labelMap.entrySet().stream()
                .map(entry -> new RiskLevelCount(entry.getKey(), entry.getValue()))
                .toArray(RiskLevelCount[]::new);
    }


    @Getter
    @Setter
    @AllArgsConstructor
    static
    class TagCount {
        private String name;
        private Long value;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    static
    class RiskLevelCount {
        private Integer riskLevel;
        private Long value;
    }

}
