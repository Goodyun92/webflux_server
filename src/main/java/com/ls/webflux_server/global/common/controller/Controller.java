package com.ls.webflux_server.global.common.controller;

import com.ls.webflux_server.global.common.model.ApiResponse;
import com.ls.webflux_server.global.common.model.ResultCode;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class Controller {
    public ApiResponse success() {
        return new ApiResponse<>(ResultCode.SUCCESS, null, null);
    }

    public <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(ResultCode.SUCCESS, null, data);
    }

    public <T> ApiResponse<T> failure(String message) {
        return new ApiResponse(ResultCode.FAILURE, message, null);
    }

    public <T> Mono<ApiResponse<T>> successMono(T data) {
        return Mono.just(success(data));
    }

    public Mono<ApiResponse> failureMono(String message) {
        return Mono.just(failure(message));
    }
}
