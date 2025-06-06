package com.ls.webflux_server.answer.model;

import lombok.*;

@Builder
@Data
public class GetSimilarityRequestDto {
    String sentence1;
    String sentence2;
}
