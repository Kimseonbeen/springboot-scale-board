package kuke.board.articleread.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OptimizedCacheLockProvider {
    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "optimized-cache-lock::";
    private static final Duration LOCK_TTL = Duration.ofSeconds(3);

    public boolean lock(String key) {
        // setIfPresent : 락을 걸기 위한 Redis 명령. 키가 없을 때만 저장
        return redisTemplate.opsForValue().setIfPresent(
                generateLockKey(key),
                "",
                LOCK_TTL    // 락 유지 시간 / 시간이 자나면 redis가 삭제
        );
    }

    public void unlock(String key) {
        redisTemplate.delete(generateLockKey(key));
    }

    private String generateLockKey(String key) {
        return KEY_PREFIX + key;
    }
}
