package kuke.board.articleread.cache;

import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import kuke.board.common.dataserializer.DataSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Component
@RequiredArgsConstructor
public class OptimizedCacheManager {
    private final StringRedisTemplate redisTemplate;
    private final OptimizedCacheLockProvider optimizedCacheLockProvider;

    private static final String DELIMITER = "::";

    public Object process(String type, long ttlSeconds, Object[] args, Class<?> returnType,
                          OptimizedCacheOriginDataSupplier<?> originDataSupplier) throws Throwable {
        String key = generateKey(type, args);   // 캐시 생성

        String cachedData = redisTemplate.opsForValue().get(key);   // Redis에 캐시 조
        if (cachedData == null) {
            return refresh(originDataSupplier, key, ttlSeconds);    // 캐시 없으면 원본 호출 후 캐시 저장
        }

        OptimizedCache optimizedCache = DataSerializer.deserialize(cachedData, OptimizedCache.class);
        if (optimizedCache == null) {
            return refresh(originDataSupplier, key, ttlSeconds);    // 역직렬화 실패 시 원본 호출 후 캐시 저장
        }

        if (!optimizedCache.isExpired()) {
            return optimizedCache.parseData(returnType);    // 캐시 유효하면 캐시 데이터 반환
        }

        if (!optimizedCacheLockProvider.lock(key)) {
            return optimizedCache.parseData(returnType);    // 락 못 걸면(다른 스레드가 갱신 중) 캐시 데이터 반환
        }

        try {
            return refresh(originDataSupplier, key, ttlSeconds);    // 락 걸었으면 원본 호출 후 캐시 갱신
        } finally {
            optimizedCacheLockProvider.unlock(key); // 락 해제
        }
    }

    private Object refresh(OptimizedCacheOriginDataSupplier<?> originDataSupplier, String key, long ttlSeconds) throws Throwable {

        Object result = originDataSupplier.get();   // refresh()에서 originDataSupplier.get() (즉, joinPoint.proceed() 호출)로 원본 데이터 호출

        OptimizedCacheTTL optimizedCacheTTL = OptimizedCacheTTL.of(ttlSeconds);
        OptimizedCache optimizedCache = OptimizedCache.of(result, optimizedCacheTTL.getLogicalTTL());

        redisTemplate.opsForValue()
                .set(
                        key,
                        DataSerializer.serialize(optimizedCache),
                        optimizedCacheTTL.getPhysicalTTL()
                );

        return result;
    }

    private String generateKey(String prefix, Object[] args) {
        return prefix + DELIMITER +
                Arrays.stream(args)
                        .map(String::valueOf)
                        .collect(joining(DELIMITER));

        // prefix = a, args = [1,2]
        // a::1::2 이렇게 생성됌
    }
}
