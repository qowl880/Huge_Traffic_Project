package com.traffic.couponservice.config;


import com.traffic.couponservice.dto.v3.CouponDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String GROUP_ID = "coupon-service";

    // Kafka Producer 설정
    @Bean
    public ProducerFactory<String, CouponDto.IssueMessage> couponProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        // Kafka 클러스터 연결 설정
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        // 키 직렬화 방식 설정  , Kafka 메시지 키를 String으로 직렬화
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // 값 직렬화 방식 설정  , CouponDto.IssueMessage 객체를 Json으로 직렬화
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Json 직렬화 시 Header에 타입 정보 추가  (나중에 consumer에서 역질화 할때 타입 정보를 사용하기 위해)
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true); 
        
        
        // 안정성을 위한 추가 설정
        // 모든 ISR의 커밋 확인을 기다림 , 최대한의 데이터 무손실 보장(성능은 약간 저하 될 수 있음)
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        // 일시적 오류(네트워크 문제 등) 발생 시 3회 재시도
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        // 동시 전송 가능한 미확인 요청 수를 1로 제한,   메세지 순서 보장을 위해 사용
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        return new DefaultKafkaProducerFactory<>(config);
    }


    @Bean
    public KafkaTemplate<String, CouponDto.IssueMessage> couponKafkaTemplate(){
        return new KafkaTemplate<>(couponProducerFactory());
    }

    // Kafka Consumer 설정
    @Bean
    public ConsumerFactory<String, CouponDto.IssueMessage> couponConsumerFactory(){
        Map<String, Object> config = new HashMap<>();
        // Kafka 클러스터 연결 설정
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        // 컨슈머 그룹 식별자 설정 , 동일한 GROUP_ID를 가진 컨슈머들은 메시지를 분산 처리
        config.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        // 키/값 역직렬화 방식 설정, Producer의 직렬화 방식과 반드시 일치해야 함
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // 안정성을 위한 추가 설정
        // 오프셋 정보가 없을 때 가장 오래된 메시지부터 시작
        // "latest"로 설정하면 새로운 메시지만 처리
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // 수동 오프셋 커밋 활성화 (Exactly-Once 처리에 필수)
        // 메시지 처리 완료 후 명시적으로 오프셋을 커밋 (consumer.commitSync()/commitAsync()) 직접 제어
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // 한 번의 poll() 호출로 가져올 최대 레코드 수
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        /* 시간 설정으로 호출 레코드 관리 가능
         config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5분
         */

        // JSON 역직렬화 세부 설정
        JsonDeserializer<CouponDto.IssueMessage> jsonDeserializer = new JsonDeserializer<>(CouponDto.IssueMessage.class);
        jsonDeserializer.addTrustedPackages("*");   // 모든 패키지를 신뢰하도록 설정 , 특정 패키지 경로 지정해주는 것이 이상적
        jsonDeserializer.setUseTypeMapperForKey(true); // 타입 매핑 활성화
        jsonDeserializer.setRemoveTypeHeaders(false);  // 헤더 유지
        /* Producer와 Consumer 모두에서 헤더에 타입 정보를 추가해 주었기 때문에 아래와 같이 @Payload를 통해 타입 검증을 통해 안전한 타입 변환이 가능함
        @KafkaListener(topics = "coupon-topic")
        public void listen(@Payload CouponDto.IssueMessage message) {
            // 자동 역직렬화
        }
         */

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),           // 키 역직렬화
                jsonDeserializer                    // 값 력직렬화
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CouponDto.IssueMessage> couponKafkaListenerContainerFactory() {
        // 컨테이너 팩토리 인스턴스 생성
        ConcurrentKafkaListenerContainerFactory<String, CouponDto.IssueMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // 컨슈머 팩토리 주입
        factory.setConsumerFactory(couponConsumerFactory());

        // 동시성 설정
        // 3개의 컨슈머 스레드 생성 , partition과 consumer는 1:1 매칭
        factory.setConcurrency(3);
        return factory;
    }
}
