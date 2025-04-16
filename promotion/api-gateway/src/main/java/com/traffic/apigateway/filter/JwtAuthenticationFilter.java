package com.traffic.apigateway.filter;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @LoadBalanced
    private final WebClient webClient;

    public JwtAuthenticationFilter(ReactorLoadBalancerExchangeFilterFunction lbFunction) {
        super(Config.class);
        this.webClient = WebClient.builder()
                .filter(lbFunction)
                .baseUrl("http://user-service")
                .build();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return validateToken(token)
                        .flatMap(userId -> proceedWithUserId(userId, exchange, chain))
                        .switchIfEmpty(chain.filter(exchange)) // If token is invalid, continue without setting userId
                        .onErrorResume(e -> handleAuthenticationError(exchange, e)); // Handle errors
            }

            return chain.filter(exchange);
        };
    }


    private Mono<Void> handleAuthenticationError(ServerWebExchange exchange, Throwable e) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    // 토큰 유효성 검사 - 외부 인증 서비스를 통해 토큰의 상태를 관리함
    private Mono<Long> validateToken(String token) {
        return webClient.post()
                .uri("/api/v1/users/validate-token")
                .bodyValue("{\"token\":\"" + token + "\"}") // 요청 본문에 JSON 형식의 데이터를 설정
                .header("Content-Type", "application/json") // 요청 헤더의 Content-Type을 application/json으로 설정하여, 요청 본문이 JSON 형식임을 명시
                .retrieve()     // 위의 api로 값을 보낸 후 결과 데이터를 받아옴
                .bodyToMono(Map.class)      // 답 본문을 Map 타입으로 변환하여 Mono로 반환
                .map(response -> Long.valueOf(response.get("id").toString()) );
    }
    

    // 백엔드 서버안에서 컴포넌트가 X-USER-ID 값을 사용할 수 있도록 저장하는 부분
    private Mono<Void> proceedWithUserId(Long userId, ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getRequest().mutate().header("X-USER-ID", String.valueOf(userId));
        return chain.filter(exchange);
    }

    public static class Config {
        // 필터 구성을 위한 설정 클래스
    }
}
