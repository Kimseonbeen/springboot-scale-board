package kuke.board.hotarticle.client;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleClient {
    private RestClient restClient;

    @Value("${endpoints.kuke-board-article-service.url")
    private String articleServiceUrl;

    // 필드 주입 (@Value) 시점 이후 안전하게 초기화하기 위해 필요
    /**
     * 스프링의 동작 순서
     * 1️⃣ 객체 생성 (생성자 호출)
     * 2️⃣ 필드 주입 (@Value, @Autowired 등) 수행 ← 여기서 articleServiceUrl 값이 들어감
     * 3️⃣ 초기화 콜백 (예: @PostConstruct) 호출
     */
    @PostConstruct
    void initRestClient() {
        restClient = RestClient.create(articleServiceUrl);
    }

    public ArticleResponse read(Long articleId) {
        try {
            return restClient.get()
                    .uri("/v1/articles/{articleId}", articleId)
                    .retrieve()
                    .body(ArticleResponse.class);
        } catch (Exception e) {
            log.error("[ArticleClient.read] articleId={}", articleId, e);
            return null;
        }
    }

    public static class ArticleResponse {
        private Long articleId;
        private String title;
        private LocalDateTime createdAt;
    }

}
