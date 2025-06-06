package com.ls.webflux_server.upload.model;

import lombok.Builder;

@Builder
public class GetCsvRequestDto {
    String s3_url;
}
