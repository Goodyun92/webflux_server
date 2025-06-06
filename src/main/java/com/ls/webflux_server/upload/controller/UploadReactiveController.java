package com.ls.webflux_server.upload.controller;

import com.ls.webflux_server.global.common.controller.Controller;
import com.ls.webflux_server.global.common.model.ApiResponse;
import com.ls.webflux_server.upload.model.UploadRequestDto;
import com.ls.webflux_server.upload.model.UploadReturnDto;
import com.ls.webflux_server.upload.service.UploadReactiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reactive/upload")
public class UploadReactiveController extends Controller {

    private final UploadReactiveService uploadReactiveService;

    @PostMapping("/start")
    public Mono<ApiResponse<UploadReturnDto>> startUploadTask(@RequestBody UploadRequestDto dto) {
        // 1. taskId 생성 UUID
        String taskId = UUID.randomUUID().toString();

        // 2. 작업 비동기 시작
        uploadReactiveService.runUploadTaskAsync(taskId, dto);

        // 3. 응답: 작업 시작 메시지 + taskId 전달
        UploadReturnDto response = UploadReturnDto.builder()
                .taskId(taskId)
                .message("PDF기반 문제 생성 작업이 시작되었습니다.")
                .build();

        return successMono(response);
    }

    // SSE 구독 엔드포인트
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> uploadStream(@RequestParam String taskId) {
        return uploadReactiveService.uploadTaskFlux(taskId);
    }
}
