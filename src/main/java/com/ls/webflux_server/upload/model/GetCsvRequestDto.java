package com.ls.webflux_server.upload.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetCsvRequestDto {
    String s3_url;
}
