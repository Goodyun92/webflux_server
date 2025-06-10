package com.ls.webflux_server.upload.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@RedisHash(value = "completed_pdf")
public class CompletedPdf {

    @Id
    private String id;

    @Indexed
    private String pdf;

    private String csv;

    @TimeToLive
    private Long expiration;

    public CompletedPdf update(String pdf, long ttl) {
        this.pdf = pdf;
        this.expiration = ttl;
        return this;
    }

    public static CompletedPdf from(String taskId, String pdf, String csv, Long expirationTime) {
        return CompletedPdf.builder()
                .id(taskId)
                .pdf(pdf)
                .csv(csv)
                .expiration(expirationTime/1000)
                .build();
    }
}
