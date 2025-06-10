package com.ls.webflux_server.upload.repository;

import com.ls.webflux_server.upload.domain.CompletedPdf;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface CompletedPdfRepository extends CrudRepository<CompletedPdf,String> {
    Optional<CompletedPdf> findByPdf(String pdf);
}
