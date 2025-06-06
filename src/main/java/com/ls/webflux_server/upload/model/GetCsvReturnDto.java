package com.ls.webflux_server.upload.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetCsvReturnDto {
    String csv_url;
}