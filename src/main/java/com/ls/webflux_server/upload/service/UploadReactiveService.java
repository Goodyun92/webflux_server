package com.ls.webflux_server.upload.service;

import com.ls.webflux_server.queue.QueueManager;
import com.ls.webflux_server.sse.BaseSseTaskService;
import com.ls.webflux_server.upload.model.GetCsvRequestDto;
import com.ls.webflux_server.upload.model.GetCsvReturnDto;
import com.ls.webflux_server.upload.model.UploadRequestDto;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * 	Mono: “단일 값” 또는 “0~1개 결과”
 * 	Flux: “여러 값” (스트림, 0~N개 결과, 예를 들면 스트리밍)
 */

@Service
public class UploadReactiveService extends BaseSseTaskService {

    private final PdfValidatorService pdfValidatorService;

    private final WebClient.Builder webClientBuilder;

    private CircuitBreaker llmCircuitBreaker;

    @Autowired
    public UploadReactiveService(QueueManager queueManager,
                                  PdfValidatorService pdfValidatorService,
                                  WebClient.Builder webClientBuilder,
                                  CircuitBreaker llmCircuitBreaker) {
        super(queueManager);
        this.pdfValidatorService = pdfValidatorService;
        this.webClientBuilder = webClientBuilder;
        this.llmCircuitBreaker = llmCircuitBreaker;
    }

    @Value("${flask.upload.base-url}")
    private String baseUrl;

    // SSE 구독: 클라이언트가 /stream?taskId=...로 구독
    public Flux<String> uploadTaskFlux(String taskId) {
        return super.taskFlux(taskId);
    }

    // LLM 작업 비동기 처리
    public void runUploadTaskAsync(String taskId, UploadRequestDto uploadRequestDto) {
        queueManager.addTask(taskId);

        // 1. URL 유효성 검사
        pdfValidatorService.isPdfValid(uploadRequestDto.getDocumentUrl())
                .subscribe(isValid -> {
                    if (!isValid) {
                        // 유효하지 않으면 SSE로 안내 후 종료
                        notifyFailure(taskId, "불가능한 PDF입니다. (2000자 미만)");
                        queueManager.removeTask(taskId);
                        return;
                    }

                    Optional.ofNullable(emitters.get(taskId)).ifPresent(e -> e.next("작업을 시작합니다."));

                    // 유효하면 LLM 로직 계속 진행 (기존 로직 호출)
                    uploadLlmProcess(taskId, uploadRequestDto);
                }, error -> {
                    notifyFailure(taskId, "PDF 유효성 검사 중 오류가 발생했습니다.");
                    queueManager.removeTask(taskId);
                });


    }

    // 실제 LLM 호출 로직
    private void uploadLlmProcess(String taskId, UploadRequestDto uploadRequestDto) {

        final GetCsvRequestDto getCsvRequestDto = GetCsvRequestDto.builder()
                .s3_url(uploadRequestDto.getDocumentUrl())
                .build();

        final WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();

        webClient.post()
                .uri("/test-s3-url")
                .bodyValue(getCsvRequestDto)
                .retrieve()
                .bodyToMono(GetCsvReturnDto.class)
                .transformDeferred(CircuitBreakerOperator.of(llmCircuitBreaker))
                // fallback: circuit breaker 열리거나 오류 발생 시 기본값(-1) 반환
                .onErrorResume(ex -> {
                    // 로그 기록 등 추가 가능
                    GetCsvReturnDto fallbackResponse = GetCsvReturnDto.builder()
                            .csv_url("fallback")
                            .build();
                    return Mono.just(fallbackResponse);
                })
                .subscribe(
                        result -> {
                            queueManager.removeTask(taskId);
                            String doneJson = result.getCsv_url().equals("fallback")
                                    ? "{\"status\":\"FAIL\",\"message\":\"LLM 서버가 일시적으로 처리 지연 중 입니다. 잠시 후 다시 시도해주세요.\"}"
                                    : "{\"status\":\"DONE\",\"csvUrl\":" + result.getCsv_url() + "}";
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
