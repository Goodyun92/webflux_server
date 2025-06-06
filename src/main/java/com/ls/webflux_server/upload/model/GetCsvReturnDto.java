package com.ls.webflux_server.upload.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetCsvReturnDto {
    String csv_url;
}