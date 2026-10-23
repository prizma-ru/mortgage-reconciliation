package com.mortgage.reconciliation.controller;

import com.mortgage.reconciliation.dto.DocBlock;
import com.mortgage.reconciliation.service.DocxReaderService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Отладочный эндпоинт для проверки парсера .docx.
 *
 * <p>Доступен только в профиле {@code dev}: запускать с
 * {@code --spring.profiles.active=dev} (или {@code -Dspring-boot.run.profiles=dev}).
 */
@Profile("dev")
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final DocxReaderService docxReaderService;

    public DebugController(DocxReaderService docxReaderService) {
        this.docxReaderService = docxReaderService;
    }

    /**
     * Разбирает загруженный .docx и возвращает список блоков в порядке документа.
     *
     * @param file загруженный файл
     * @return блоки документа
     */
    @PostMapping(value = "/docx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<DocBlock> readDocx(@RequestParam("file") MultipartFile file) {
        try (InputStream inputStream = DocxUploadSupport.requireDocxStream(file)) {
            return docxReaderService.readDocx(inputStream);
        } catch (IOException ex) {
            throw DocxUploadSupport.invalidDocx(ex);
        }
    }
}
