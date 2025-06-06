package com.ls.webflux_server.global.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class ApiResponse<T> {

    private ResultCode resultCode;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String resultMessage;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    // Response
    public ApiResponse(ResultCode resultCode, String resultMessage, T data) {
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        this.data = data;
    }

}
