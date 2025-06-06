package com.ls.webflux_server.upload.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadReturnDto {
    String taskId;

    String message;
}
