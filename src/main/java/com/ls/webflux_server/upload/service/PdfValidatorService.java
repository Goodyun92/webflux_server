package com.ls.webflux_server.upload.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileOutputStream;

import static java.lang.Boolean.FALSE;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfValidatorService {

    private final WebClient.Builder webClientBuilder;


    public Mono<Boolean> isPdfValid(String pdfUrl) {

        log.info("isPdfValid called with pdfUrl: {}", pdfUrl);

        // 확장자 검사 (대소문자 무시, 공백 제거)
        if (pdfUrl == null || !pdfUrl.trim().toLowerCase().endsWith(".pdf")) {
            return Mono.just(FALSE);
        }

        final WebClient webClient = webClientBuilder.baseUrl(pdfUrl).build();

        // 1. WebClient로 PDF 다운로드 (논블로킹)
        return webClient.get()
                .retrieve()
                .bodyToMono(byte[].class)
                .flatMap(bytes -> Mono.fromCallable(() -> {
                    // 2. 임시파일로 저장
                    File tempFile = File.createTempFile("download-", ".pdf");
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        fos.write(bytes);
                        fos.flush();
                    }

                    // 3. PDFBox로 텍스트 추출 및 2000자 이상인지 검사
                    try (PDDocument document = PDDocument.load(tempFile)) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        String text = stripper.getText(document);
                        int length = text.length();
                        return length >= 2000;
                    } finally {
                        tempFile.delete();
                    }
                }).subscribeOn(Schedulers.boundedElastic()));
    }
}
