package kuke.board.hotarticle.api;

import kuke.board.hotarticle.service.response.HotArticleResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;

public class HotArticleApiTest {
    RestClient restClient = RestClient.create("http://localhost:9004");

    @Test
    void readAllTest() {
        List<HotArticleResponse> responses = restClient.get()
                .uri("/v1/hot-articles/articles/date/{dateStr}", "20250731")
                .retrieve()
                .body(new ParameterizedTypeReference<List<HotArticleResponse>>() {
                });

        System.out.println("HotArticleApiTest.readAllTest1");
        System.out.println("responses = " + responses);

        for (HotArticleResponse response : responses) {
            System.out.println("HotArticleApiTest.readAllTest2");
            System.out.println("response = " + response);
        }
    }
}
