package kuke.board.hotarticle.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TimeCalculatorUtils {

    public static Duration calculateDurationToMidnight() {
        LocalDateTime now = LocalDateTime.now();    // 오늘
        LocalDateTime midnight = now.plusDays(1).with(LocalTime.MIDNIGHT);  // 다음 날의 12:00 자정
        return Duration.between(now, midnight); // 오늘과 자정 간의 남은 시간
    }
}
