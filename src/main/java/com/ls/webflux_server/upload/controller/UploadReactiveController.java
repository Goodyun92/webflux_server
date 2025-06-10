package com.ls.webflux_server.upload.controller;

import com.ls.webflux_server.upload.model.UploadRequestDto;
import com.ls.webflux_server.global.common.model.PrepReturnDto;
import com.ls.webflux_server.upload.service.UploadReactiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reactive/upload")
public class UploadReactiveController {

    private final UploadReactiveService uploadReactiveService;

    @PostMapping("/prep")
    public Mono<PrepReturnDto> UploadPrep(@RequestBody UploadRequestDto dto) {
        return uploadReactiveService.uploadPrep(dto);
    }

    // SSE 구독 엔드포인트
    @GetMapping(value = "/stream/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> uploadStream(@PathVariable String taskId) {
        return uploadReactiveService.uploadTaskFlux(taskId);
    }
}
