package com.traffic.apigateway.config;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    // Redis를 사용하여 요청 제한을 구현
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // replenishRate : 초당 허용되는 요청 수
        // burstCapacity : 최대 누적 가능한 요청 수
        return new RedisRateLimiter(10,20);
    }

    // 요청에 대한 고유한 키를 생성하는 역할을 합니다. 이 키는 요청 제한을 적용할 때 사용됩니다.
    // 헤더가 존재하면 User-ID값 사용, 헤더가 없다면 요청자 IP 주소 사용
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest().getHeaders().getFirst("X-User-ID") != null ?
                        exchange.getRequest().getHeaders().getFirst("X-User-ID") :
                        exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }
}
