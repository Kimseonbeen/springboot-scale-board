package kuke.board.articleread.cache;

import lombok.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class OptimizedCacheTest {
    @Test
    void parseDataTest() {
        parseDataTest("data", 10);
        parseDataTest(3L, 10);
        parseDataTest(3, 10);

        // optimizedCache = OptimizedCache(data={"testData":"hihi"}, expiredAt=2025-08-03T19:16:39.539798)
        parseDataTest(new TestClass("hihi"), 10);
    }

    void parseDataTest(Object data, long ttlSeconds) {
        // given
        OptimizedCache optimizedCache = OptimizedCache.of(data, Duration.ofSeconds(ttlSeconds));
        System.out.println("optimizedCache = " + optimizedCache);

        // when
        Object resolveData = optimizedCache.parseData(data.getClass());

        // then
        System.out.println("resolveData = " + resolveData);
        System.out.println("resolveData.getClass() = " + resolveData.getClass());
        assertThat(resolveData).isEqualTo(data);
    }

    @Test
    void isExpiredTest() {
        assertThat(OptimizedCache.of("data", Duration.ofDays(-30)).isExpired()).isTrue();
        assertThat(OptimizedCache.of("data", Duration.ofDays(30)).isExpired()).isFalse();
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestClass {
        String testData;
    }

}