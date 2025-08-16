package kuke.board.articleread.repository;

import kuke.board.common.dataserializer.DataSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Repository
@RequiredArgsConstructor
public class ArticleQueryModelRepository {
    private final StringRedisTemplate redisTemplate;

    // article-read::article::{articleId}
    private static final String KEY_FORMAT = "article-read::article::%s";

    public void create(ArticleQueryModel articleQueryModel, Duration ttl) {
        redisTemplate.opsForValue()
                .set(generateKey(articleQueryModel), DataSerializer.serialize(articleQueryModel), ttl);
    }

    public void update(ArticleQueryModel articleQueryModel) {
        redisTemplate.opsForValue().setIfPresent(generateKey(articleQueryModel), DataSerializer.serialize(articleQueryModel));
    }

    public void delete(Long articleId) {
        redisTemplate.delete(generateKey(articleId));
    }

    public Optional<ArticleQueryModel> read(Long articleId) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(generateKey(articleId))
        ).map(json -> DataSerializer.deserialize(json, ArticleQueryModel.class));
    }

    private String generateKey(ArticleQueryModel articleQueryModel) {
        return generateKey(articleQueryModel.getArticleId());
    }

    private String generateKey(Long articleId) {
        return KEY_FORMAT.formatted(articleId);
    }

    /**
     * 이 메서드는 게시글 ID 목록 articleIds를 받아,
     * Redis에 저장된 JSON 형태의 캐시 데이터를 조회한 뒤,
     * 역직렬화해서 ArticleQueryModel 객체로 만들고,
     * 그걸 Map<articleId, ArticleQueryModel> 형태로 반환합니다.
     */
    public Map<Long, ArticleQueryModel> readAll(List<Long> articleIds) {
        List<String> keyList = articleIds.stream().map(this::generateKey).toList();
        /**
         * multiGet :
         * Redis에 여러 key를 한 번에 조회하는 명령어입니다.
         * 예를 들어: multiGet(["article:1", "article:2", "article:3"])
         * 결과는 List<String>으로 반환됩니다. 각각은 JSON 문자열입니다.
         */
        return redisTemplate.opsForValue().multiGet(keyList).stream()
                .filter(Objects::nonNull)
                // .map(...)은 Stream 안의 요소를 다른 것으로 바꾸는 함수
                .map(json -> DataSerializer.deserialize(json, ArticleQueryModel.class))
                .collect(toMap(ArticleQueryModel::getArticleId, identity()));
    }
}
