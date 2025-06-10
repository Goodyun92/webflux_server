package com.ls.webflux_server.upload.service;

import com.ls.webflux_server.upload.domain.CompletedPdf;
import com.ls.webflux_server.upload.repository.CompletedPdfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompletedPdfService {

    public static final Long PDF_CACHE_EXPIRE_TIME = 1000 * 60 * 60 * 24L; // 24시간 TTL

    private final CompletedPdfRepository completedPdfRepository;

    public void saveCompletedPdf(String taskId, String documentUrl, String csvUrl){
        completedPdfRepository.save(
                CompletedPdf.from(taskId, documentUrl, csvUrl, PDF_CACHE_EXPIRE_TIME)
        );
    }

    public Optional<CompletedPdf> getCompletedPdfByDocumentUrl(String documentUrl){
        return completedPdfRepository.findByPdf(documentUrl);
    }
}
