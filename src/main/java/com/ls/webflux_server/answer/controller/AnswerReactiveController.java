package com.ls.webflux_server.answer.controller;

import com.ls.webflux_server.answer.model.AnswerRequestDto;
import com.ls.webflux_server.answer.service.AnswerReactiveService;
import com.ls.webflux_server.global.common.controller.Controller;
import com.ls.webflux_server.global.common.model.ApiResponse;
import com.ls.webflux_server.upload.model.UploadReturnDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reactive/answer")
public class AnswerReactiveController extends Controller {

    private final AnswerReactiveService answerReactiveService;

    @PostMapping("/start")
    public Mono<ApiResponse<UploadReturnDto>> startAnswerTask(@RequestBody AnswerRequestDto dto) {
        // 1. taskId 생성 UUID
        String taskId = UUID.randomUUID().toString();

        // 2. 작업 비동기 시작
        answerReactiveService.runAnswerTaskAsync(taskId, dto);

        // 3. 응답: 작업 시작 메시지 + taskId 전달
        UploadReturnDto response = UploadReturnDto.builder()
                .taskId(taskId)
                .message("답변 유사도 채점 작업이 시작되었습니다.")
                .build();

        return successMono(response);
    }

    // SSE 구독 엔드포인트
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> uploadStream(@RequestParam String taskId) {
        return answerReactiveService.answerTaskFlux(taskId);
    }
}
