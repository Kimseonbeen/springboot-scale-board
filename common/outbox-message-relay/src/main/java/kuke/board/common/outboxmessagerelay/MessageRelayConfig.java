package kuke.board.common.outboxmessagerelay;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * kuke.board.common.outboxmessagerelay 패키지에 컴포넌트 클래스가 있고
 * MessageRelayConfig가 그걸 스캔해줘야 하고
 * 이 설정 클래스를 AutoConfiguration.imports로 등록하면
 * 해당 모듈(JAR)을 참조하는 다른 Spring Boot 애플리케이션이 MessageRelayConfig를 자동으로 불러오게 됨
 */

@EnableAsync
@Configuration
@ComponentScan("kuke.board.common.outboxmessagerelay")
@EnableScheduling
public class MessageRelayConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaTemplate<String, String> messageRelayKafkaTemplate() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(configProps));
    }

    // 트랜잭션이 끝날떄 마다 이벤트 전송을 비동기로 전송하기 위해 처리하는 스레드 풀
    @Bean
    public Executor messageRelayPublishEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mr-pub-event-");
        return executor;
    }

    // Kafka 등으로 전송되지 못한 이벤트를 주기적으로 재시도하기 위한 Executor
    // 싱글 스레드로 순차 처리하여 중복 전송을 방지함
    @Bean
    public Executor messageRelayPublishPendingEventExecutor() {
        return Executors.newSingleThreadScheduledExecutor(); // 싱글스레드로만 미 전송 이벤트를 전송하겠음
    }
}
