package com.mortgage.reconciliation.controller;

import com.mortgage.reconciliation.dto.DocBlock;
import com.mortgage.reconciliation.service.DocxReaderService;
import org.springframework.http.HttpStatus;
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
import java.util.Locale;

/**
 * Отладочный эндпоинт для проверки парсера .docx.
 *
 * <p>Разрешён в {@code CorsConfig} (правило {@code /**}) для фронтенда
 * на {@code http://localhost:5173} и {@code http://127.0.0.1:5173}.
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private static final String DOCX_EXTENSION = ".docx";

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
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required and must not be empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(DOCX_EXTENSION)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .docx files are supported");
        }

        try (InputStream inputStream = file.getInputStream()) {
            return docxReaderService.readDocx(inputStream);
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to read .docx file: " + ex.getMessage(),
                    ex
            );
        }
    }
}
