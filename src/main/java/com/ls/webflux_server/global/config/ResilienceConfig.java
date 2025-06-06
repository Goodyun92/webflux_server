package com.ls.webflux_server.global.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Value("${resilience4j.circuitbreaker.configs.default.slidingWindowSize}")
    private Integer slidingWindowSize;

    @Value("${resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls}")
    private Integer minimumNumberOfCalls;

    @Value("${resilience4j.circuitbreaker.configs.default.failureRateThreshold}")
    private Integer failureRateThreshold;

    @Value("${resilience4j.circuitbreaker.configs.default.waitDurationInOpenState}")
    private Duration waitDurationInOpenState;

    @Bean
    public CircuitBreaker llmCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold) // 실패 비율 50% 이상이면 OPEN
                .waitDurationInOpenState(waitDurationInOpenState) // OPEN 후 30초 대기
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED) // COUNT_BASED: 요청 횟수 기반, TIME_BASED: 시간 기반
                .slidingWindowSize(slidingWindowSize) // 최근 10번 요청 기준
                .minimumNumberOfCalls(minimumNumberOfCalls) // 최소 5회 이상 시 OPEN 후보
//                .permittedNumberOfCallsInHalfOpenState(3) // HALF-OPEN 시 허용 요청 수
//                .recordExceptions(java.io.IOException.class, java.util.concurrent.TimeoutException.class) // 어떤 Exception을 "실패"로 기록할지
//                .ignoreExceptions(IllegalArgumentException.class) // 무시할 Exception 지정
                .build();

        return CircuitBreaker.of("llm-cb-custom", config);
    }
}
