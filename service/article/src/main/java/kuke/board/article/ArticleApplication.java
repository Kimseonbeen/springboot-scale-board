package kuke.board.article;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * kuke.board
 * ├── common
 * │   └── outboxmessagerelay
 * │       ├── Outbox.java  <-- @Entity
 * │       └── OutboxRepository.java  <-- JpaRepository
 * ├── article
 * │   └── Article.java
 * └── view
 *     └── ViewApplication.java <-- 여기서 SpringBootApplication
 *
 * 이렇게 하면 스프링이 kuke.board.outbox 패키지까지 올라가서:
 * @Entity가 붙은 Outbox 클래스 → JPA에 등록되고
 * OutboxRepository → Spring Data JPA에서 구현체 생성
 * 그래서 service/article 모듈이 common/outbox-message-relay 모듈의 JPA 엔티티와 레포지토리를 문제 없이 사용할 수 있게 되는 것이야.
 *
 *  implementation으로 클래스를 쓸 수 있게 만들고,
 *  @EntityScan, @EnableJpaRepositories로 Spring이 그걸 인식하게 만든다.
 *
 *  ✅ 정리: 두 가지를 같이 봐야 이해됨
 *  1️⃣ implementation project(':common:outbox-message-relay')
 *  이건 Gradle의 의존성 선언
 *
 * service/article 모듈에서 common/outbox-message-relay 모듈의 클래스들을 컴파일 및 런타임에 사용할 수 있도록 연결하는 작업
 * 즉, 이걸 추가함으로써 코드 차원에서는 Outbox, OutboxRepository를 import하고 사용할 수 있게 됨.
 *
 * 2️⃣ @EntityScan("kuke.board"), @EnableJpaRepositories("kuke.board")
 * 이건 Spring이 런타임에 빈을 자동으로 등록하기 위한 설정이야.
 * 위 1번으로 클래스는 접근할 수 있지만, Spring은 기본적으로 애플리케이션 클래스 하위 패키지만 스캔하니까 별도로 이 어노테이션으로 스캔 범위를 넓혀줘야 함.
 */
@EntityScan(basePackages = "kuke.board")
@SpringBootApplication
@EnableJpaRepositories(basePackages = "kuke.board")
public class ArticleApplication {
    public static void main(String[] args) {
        SpringApplication.run(ArticleApplication.class, args);
    }
}
