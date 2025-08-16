package kuke.board.articleread.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.springframework.data.redis.connection.Limit.limit;

@Repository
@RequiredArgsConstructor
public class ArticleIdListRepository {
    private final StringRedisTemplate redisTemplate;

    //article-read::board::{boardId}::article-list
    private static final String KEY_FORMAT = "article-read::board::%s::article-list";

    public void add(Long boardId, Long articleId, Long limit) {
        redisTemplate.executePipelined((RedisCallback<?>) action -> {
            StringRedisConnection conn = (StringRedisConnection) action;
            String key = generateKey(boardId);
            // score를 고정값을 뒤어 articleId를 오름차순 정렬된 상태를 유지함
            conn.zAdd(key, 0, toPaddedString(articleId));
            conn.zRemRange(key, 0, - limit - 1);    // 상위 limit 개수 유지
            return null;
        });
    }

    public void delete(Long boardId, Long articleId) {
        redisTemplate.opsForZSet().remove(generateKey(boardId), toPaddedString(articleId));
    }

    public List<Long> readAll(Long boardId, Long offset, Long limit) {
        return redisTemplate.opsForZSet()
                .reverseRange(generateKey(boardId), offset, offset + limit - 1)
                .stream().map(Long::valueOf).toList();
    }

    public List<Long> readAllInfiniteScroll(Long boardId, Long lastArticleId, Long limit) {
        return redisTemplate.opsForZSet().reverseRangeByLex(
                generateKey(boardId),
                lastArticleId == null ?
                        Range.unbounded() :
                        Range.leftUnbounded(Range.Bound.exclusive(toPaddedString(lastArticleId))),
                limit().count(limit.intValue())
        ).stream().map(Long::valueOf).toList();
    }

    // Long 값을 고정된 자리수의 문자로 바꿔줌
    private String toPaddedString(Long articleId) {
        return "%019d".formatted(articleId);
        // 1234 -> 0000000000000001234
    }

    private String generateKey(Long boardId) {
        return KEY_FORMAT.formatted(boardId);
    }
}
