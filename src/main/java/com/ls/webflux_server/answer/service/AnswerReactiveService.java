package com.ls.webflux_server.answer.service;

import com.ls.webflux_server.answer.model.AnswerRequestDto;
import com.ls.webflux_server.answer.model.GetSimilarityRequestDto;
import com.ls.webflux_server.global.common.model.PrepReturnDto;
import com.ls.webflux_server.sse.BaseSseTaskService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerReactiveService extends BaseSseTaskService {

    private final WebClient.Builder webClientBuilder;

    private final CircuitBreaker llmCircuitBreaker;

    @Value("${flask.similarity.base-url}")
    private String baseUrl;

    private final Map<String, AnswerRequestDto> answerTask = new ConcurrentHashMap<>();

    public Mono<PrepReturnDto> answerPrep(AnswerRequestDto dto) {
        String taskId = UUID.randomUUID().toString();

        answerTask.put(taskId, dto);

        PrepReturnDto response = PrepReturnDto.builder()
                .taskId(taskId)
                .message("SSE 연결로 작업을 시작하세요.")
                .build();

        return Mono.just(response);
    }

    // SSE 구독: 클라이언트가 /stream?taskId=...로 구독
    public Flux<String> answerTaskFlux(String taskId) {
        final AnswerRequestDto answerRequestDto = answerTask.get(taskId);

        if (answerRequestDto == null) {
            return Flux.error(new IllegalArgumentException("유효하지 않은 taskId: " + taskId));
        }

        waitingQueue.add(taskId);

        return Flux.create(emitter -> {
            emitters.put(taskId, emitter);

            Optional.ofNullable(emitters.get(taskId)).ifPresent(e -> e.next("작업을 시작합니다."));

            answerLlmProcess(taskId, answerRequestDto);

            Disposable intervalTask = Flux.interval(Duration.ZERO, Duration.ofSeconds(2))
                    .subscribe(tick -> Optional.ofNullable(emitters.get(taskId))
                            .ifPresent(e -> e.next("{\"status\":\"WAITING\",\"queue\":" + getMyWaitingCount(taskId) + "}")));

            emitter.onDispose(() -> {
                intervalTask.dispose();
                emitters.remove(taskId);
                answerTask.remove(taskId);
                waitingQueue.remove(taskId);
            });
        });
    }

    private void answerLlmProcess(String taskId, AnswerRequestDto answerRequestDto) {
        final GetSimilarityRequestDto requestDto = GetSimilarityRequestDto.builder()
                .sentence1(answerRequestDto.getCorrectAnswer())
                .sentence2(answerRequestDto.getUserAnswer())
                .build();

        final WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();

        webClient.post()
                .uri("/get-similarity")
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(Integer.class)
                .transformDeferred(CircuitBreakerOperator.of(llmCircuitBreaker))
                // fallback: circuit breaker 열리거나 오류 발생 시 기본값(-1) 반환
                .onErrorResume(ex -> {
                    // 로그 기록 등 추가 가능
                    if (ex instanceof CallNotPermittedException) {
                        // 서킷브레이커 OPEN 등
                        log.error("서킷브레이커 OPEN: {}", ex.getMessage(), ex);
                        return Mono.just(-1);
                    } else {
                        // 그 외 예외
                        log.error("LLM 호출 에러: {}", ex.getMessage(), ex);
                        return Mono.just(-2);
                    }
                })
                .subscribe(
                        result -> {
                            log.info("LLM 응답: {}", result);
                            String doneJson = switch (result) {
                                case -1 ->
                                        "{\"status\":\"FAIL\",\"reason\":\"CIRCUIT_BREAKER\",\"message\":\"LLM 서버가 일시적으로 차단되었습니다. 잠시 후 다시 시도해주세요.\"}";
                                case -2 ->
                                        "{\"status\":\"FAIL\",\"reason\":\"LLM_ERROR\",\"message\":\"LLM 서버 호출 중 오류가 발생했습니다.\"}";
                                default -> "{\"status\":\"DONE\",\"score\":" + result + "}";
                            };
                            super.notifyCompletion(taskId, doneJson);

                            // 작업 완료 후 taskId 제거
                            answerTask.remove(taskId);
                            waitingQueue.remove(taskId);
                        },
                        error -> {
                            // LLM 처리 실패
                            log.error("LLM 에러: {}", error.getMessage(), error);
                            super.notifyFailure(taskId, error.getMessage());
                            // 작업 완료 후 taskId 제거
                            answerTask.remove(taskId);
                            waitingQueue.remove(taskId);
                        }
                );
    }
}
