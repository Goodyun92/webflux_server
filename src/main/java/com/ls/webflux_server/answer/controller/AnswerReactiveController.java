package com.ls.webflux_server.answer.controller;

import com.ls.webflux_server.answer.model.AnswerRequestDto;
import com.ls.webflux_server.answer.service.AnswerReactiveService;
import com.ls.webflux_server.global.common.controller.Controller;
import com.ls.webflux_server.global.common.model.PrepReturnDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reactive/answer")
public class AnswerReactiveController extends Controller {

    private final AnswerReactiveService answerReactiveService;

    @PostMapping("/prep")
    public Mono<PrepReturnDto> AnswerPrep(@RequestBody AnswerRequestDto dto) {
        return answerReactiveService.answerPrep(dto);
    }

    // SSE 구독 엔드포인트
    @GetMapping(value = "/stream/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> uploadStream(@PathVariable String taskId) {
        return answerReactiveService.answerTaskFlux(taskId);
    }
}
