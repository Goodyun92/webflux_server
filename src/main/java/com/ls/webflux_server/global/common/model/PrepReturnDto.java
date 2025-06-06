package com.ls.webflux_server.global.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PrepReturnDto {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    String taskId;

    String message;
}
