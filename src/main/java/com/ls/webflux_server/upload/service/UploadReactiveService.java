package com.ls.webflux_server.upload.service;

import com.ls.webflux_server.sse.BaseSseTaskService;
import com.ls.webflux_server.upload.model.GetCsvRequestDto;
import com.ls.webflux_server.upload.model.GetCsvReturnDto;
import com.ls.webflux_server.upload.model.UploadRequestDto;
import com.ls.webflux_server.global.common.model.PrepReturnDto;
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

/**
 * 	Mono: “단일 값” 또는 “0~1개 결과”
 * 	Flux: “여러 값” (스트림, 0~N개 결과, 예를 들면 스트리밍)
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadReactiveService extends BaseSseTaskService {

    private final PdfValidatorService pdfValidatorService;

    private final WebClient.Builder webClientBuilder;

    private final CircuitBreaker llmCircuitBreaker;

    @Value("${flask.upload.base-url}")
    private String baseUrl;

    private final Map<String, String> uploadTask = new ConcurrentHashMap<>();

    public Mono<PrepReturnDto> validatePdf(UploadRequestDto dto) {
//        임시 주석 처리
//        return pdfValidatorService.isPdfValid(dto.getDocumentUrl())
//                .map(valid -> {
//                    if (!valid) {
//                        return PrepReturnDto.builder()
//                                .message("불가능한 PDF입니다. (2000자 미만)")
//                                .build();
//                    } else {
//                        String taskId = UUID.randomUUID().toString();
//                        uploadTask.put(taskId, dto.getDocumentUrl());
//
//                        return PrepReturnDto.builder()
//                                .taskId(taskId)
//                                .message("유효한 PDF입니다. SSE 연결로 작업을 시작하세요.")
//                                .build();
//                    }
//                });

        // 테스트용
        String taskId = UUID.randomUUID().toString();
        uploadTask.put(taskId, dto.getDocumentUrl());

        return Mono.just(
                PrepReturnDto.builder()
                        .taskId(taskId)
                        .message("유효한 PDF입니다. SSE 연결로 작업을 시작하세요.")
                        .build()
        );
    }

    // SSE 구독: 클라이언트가 /stream?taskId=...로 구독
    public Flux<String> uploadTaskFlux(String taskId) {
        final String documentUrl = uploadTask.get(taskId);

        if (documentUrl == null) {
            return Flux.error(new IllegalArgumentException("유효하지 않은 taskId입니다: " + taskId));
        }

        waitingQueue.add(taskId);

        return Flux.create(emitter -> {
            emitters.put(taskId, emitter);

            Optional.ofNullable(emitters.get(taskId)).ifPresent(e -> e.next("작업을 시작합니다."));

            uploadLlmProcess(taskId,documentUrl);

            Disposable intervalTask = Flux.interval(Duration.ZERO, Duration.ofSeconds(2))
                    .subscribe(tick -> Optional.ofNullable(emitters.get(taskId))
                            .ifPresent(e -> e.next("{\"status\":\"WAITING\",\"queue\":" + getMyWaitingCount(taskId) + "}")));

            emitter.onDispose(() -> {
                intervalTask.dispose();
                emitters.remove(taskId);
                uploadTask.remove(taskId);
                waitingQueue.remove(taskId);
            });
        });

    }

    // 실제 LLM 호출 로직
    public void uploadLlmProcess(String taskId, String documentUrl) {

        final GetCsvRequestDto getCsvRequestDto = GetCsvRequestDto.builder()
                .s3_url(documentUrl)
                .build();

        final WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();

        log.info("baseurl: {}", baseUrl);

        webClient.post()
                .uri("/test-s3-url")
                .bodyValue(getCsvRequestDto)
                .retrieve()
                .bodyToMono(GetCsvReturnDto.class)
                .transformDeferred(CircuitBreakerOperator.of(llmCircuitBreaker))
                .onErrorResume(ex -> {
                    if (ex instanceof CallNotPermittedException) {
                        // 서킷브레이커 OPEN 등
                        log.error("서킷브레이커 OPEN: {}", ex.getMessage(), ex);
                        GetCsvReturnDto fallbackResponse = GetCsvReturnDto.builder()
                                .csv_url("circuitbreaker_open")
                                .build();
                        return Mono.just(fallbackResponse);
                    } else {
                        // 그 외 예외
                        log.error("LLM 호출 에러: {}", ex.getMessage(), ex);
                        GetCsvReturnDto fallbackResponse = GetCsvReturnDto.builder()
                                .csv_url("llm_error")
                                .build();
                        return Mono.just(fallbackResponse);
                    }
                })
                .subscribe(
                        result -> {
                            log.info("LLM 응답: {}", result);
                            String doneJson = switch (result.getCsv_url()) {
                                case "circuitbreaker_open" ->
                                        "{\"status\":\"FAIL\",\"reason\":\"CIRCUIT_BREAKER\",\"message\":\"LLM 서버가 일시적으로 차단되었습니다. 잠시 후 다시 시도해주세요.\"}";
                                case "llm_error" ->
                                        "{\"status\":\"FAIL\",\"reason\":\"LLM_ERROR\",\"message\":\"LLM 서버 호출 중 오류가 발생했습니다.\"}";
                                default -> "{\"status\":\"DONE\",\"csvUrl\":\"" + result.getCsv_url() + "\"}";
                            };
                            super.notifyCompletion(taskId, doneJson);

                            // 작업 완료 후 taskId 제거
                            uploadTask.remove(taskId);
                            waitingQueue.remove(taskId);
                        },
                        error -> {
                            // subscribe 콜백에서 도달하는 건 거의 불가(위에서 Mono.just로 끝냈으니까)
                            log.error("LLM 에러: {}", error.getMessage(), error);
                            super.notifyFailure(taskId, error.getMessage());
                            uploadTask.remove(taskId);
                            waitingQueue.remove(taskId);
                        }
                );
    }

}
