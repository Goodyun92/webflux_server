package com.ls.webflux_server.answer.service;

import com.ls.webflux_server.answer.model.AnswerRequestDto;
import com.ls.webflux_server.answer.model.GetSimilarityRequestDto;
import com.ls.webflux_server.queue.QueueManager;
import com.ls.webflux_server.sse.BaseSseTaskService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@Slf4j
public class AnswerReactiveService extends BaseSseTaskService {

    private final WebClient.Builder webClientBuilder;

    private final CircuitBreaker llmCircuitBreaker;

    @Autowired
    public AnswerReactiveService(QueueManager queueManager, WebClient.Builder webClientBuilder, CircuitBreaker llmCircuitBreaker) {
        super(queueManager);
        this.webClientBuilder = webClientBuilder;
        this.llmCircuitBreaker = llmCircuitBreaker;
    }

    @Value("${flask.similarity.base-url}")
    private String baseUrl;

    // SSE 구독: 클라이언트가 /stream?taskId=...로 구독
    public Flux<String> answerTaskFlux(String taskId) {
        return super.taskFlux(taskId);
    }

    // LLM 작업 비동기 처리
    public void runAnswerTaskAsync(String taskId, AnswerRequestDto answerRequestDto) {
        queueManager.addTask(taskId);

        Optional.ofNullable(emitters.get(taskId)).ifPresent(e -> e.next("작업을 시작합니다."));

        answerLlmProcess(taskId, answerRequestDto);
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
                    return Mono.just(-1);
                })
                .subscribe(
                        result -> {
                            queueManager.removeTask(taskId);
                            String doneJson = result == -1
                                    ? "{\"status\":\"FAIL\",\"message\":\"LLM 서버가 일시적으로 처리 지연 중 입니다. 잠시 후 다시 시도해주세요.\"}"
                                    : "{\"status\":\"DONE\",\"score\":" + result + "}";
                            super.notifyCompletion(taskId, doneJson);
                        },
                        error -> {
                            // LLM 처리 실패
                            queueManager.removeTask(taskId);
                            super.notifyFailure(
                                    taskId, error.getMessage()
                            );
                        }
                );
    }
}
