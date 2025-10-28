package com.unicorn.journey.assistant.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class RelativeTimeConverter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 获取更详细的相对时间（包括秒）
     */
    public static String convert(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return "未知时间";
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateString, FORMATTER);
            LocalDateTime now = LocalDateTime.now();

            if (dateTime.isAfter(now)) {
                return "未来时间";
            }

            Duration duration = Duration.between(dateTime, now);
            long days = duration.toDays();
            long hours = duration.toHours() % 24;
            long minutes = duration.toMinutes() % 60;
            long seconds = duration.getSeconds() % 60;

            if (days > 0) {
                return days + "天" + hours + "小时" + minutes + "分钟前";
            } else if (hours > 0) {
                return hours + "小时" + minutes + "分钟前";
            } else if (minutes > 0) {
                return minutes + "分钟" + seconds + "秒前";
            } else {
                return "刚刚";
            }
        } catch (DateTimeParseException e) {
            return dateString;
        }
    }
}
