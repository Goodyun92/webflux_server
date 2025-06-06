package com.ls.webflux_server.global.common.model;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ResultCode {
    SUCCESS(HttpStatus.OK),
    FAILURE(),
    NOTFOUND(),
    CONFLICT(HttpStatus.CONFLICT),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    BAD_REQUEST(HttpStatus.BAD_REQUEST);

    final HttpStatus httpStatus;

    ResultCode() {
        httpStatus = HttpStatus.OK;
    }

    ResultCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}
