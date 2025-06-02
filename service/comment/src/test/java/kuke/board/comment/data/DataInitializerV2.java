package kuke.board.comment.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kuke.board.comment.entity.Comment;
import kuke.board.comment.entity.CommentPath;
import kuke.board.comment.entity.CommentV2;
import kuke.board.common.snowflake.Snowflake;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class DataInitializerV2 {

    /**
     * 이렇게 하면, 직접 데이터베이스 접근(EntityManager) 후 프로그래매틱 트랜잭션 안에서 처리하고,
     * 각 작업별로 유니크 ID를 생성하고, 멀티스레드 작업을 CountDownLatch로 동기화하는 구조를 구현할 수 있습니다.
     */

    @PersistenceContext
    EntityManager entityManager;
    @Autowired
    TransactionTemplate transactionTemplate;
    Snowflake snowflake = new Snowflake();
    CountDownLatch latch = new CountDownLatch(EXECUTE_COUNT);    // 스레드 간 작업 완료 시점 동기

    static final int BULK_INSERT_SIZE = 2000;
    static final int EXECUTE_COUNT = 6000;

    /**
     *
     * 메인 스레드: ──submit(task1)──▶
     *                ──submit(task2)──▶
     *                ──submit(task3)──▶
     *                for문 종료 ▶ latch.await()
     *
     * 워커 스레드1: ──(insert task1 실행)──▶ latch.countDown()
     * 워커 스레드2: ──(insert task2 실행)──▶ latch.countDown()
     * 워커 스레드3: ──(insert task3 실행)──▶ latch.countDown()
     *
     * 제출(submit) 시점에는 작업이 큐에 들어가기만 하고
     * 워커 스레드가 남아서 하나씩 꺼내 실행
     * 메인 스레드는 latch.await() 로 모든 작업 완료 시점까지 블록
     *
     */

    @Test
    void initialize() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10); // 최대 10개의 스레드를 가지는 고정 크기(thread-fixed) 풀을 생성

        // (1) for 문 돌면서 작업 6000건을 제출 → 즉시 끝난다
        for (int i = 0; i < EXECUTE_COUNT; i++) {
            int start = i * BULK_INSERT_SIZE;
            int end = (i + 1) * BULK_INSERT_SIZE;
            executorService.submit(() -> {
                insert(start, end);    // 백그라운드 스레드에서 실행
                latch.countDown();
                System.out.println("latch.getCount() = " + latch.getCount());
            });
        }
        // 여기까지는 '제출'만 끝난 상황!


        // (2) CountDownLatch가 6000번 countDown 될 때까지 블록
        latch.await();
        // 이 시점에야 “모든 insert() 작업이 진짜 끝났다”를 보장

        // (3) 스레드풀도 깔끔히 종료
        executorService.shutdown();
    }

    void insert(int start, int end) {
        transactionTemplate.executeWithoutResult(status -> {
            Comment prev = null;

            for (int i = start; i < end; i++) {
                CommentV2 comment = CommentV2.create(
                        snowflake.nextId(),
                        "content",
                        1L,
                        1L,
                        toPath(i)
                );
                entityManager.persist(comment);
            }
        });
    }

    private static final String CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final int DEPTH_CHUNK_SIZE = 5;

    CommentPath toPath(int value) {
        String path = "";
        for (int i=0; i < DEPTH_CHUNK_SIZE; i++) {
            path = CHARSET.charAt(value % CHARSET.length()) + path;
            value /= CHARSET.length();
        }
        return CommentPath.create(path);
    }
}
