package com.mortgage.reconciliation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Общая проверка загружаемого .docx для контроллеров.
 */
final class DocxUploadSupport {

    private static final String DOCX_EXTENSION = ".docx";

    private DocxUploadSupport() {
    }

    /**
     * Проверяет файл и открывает его содержимое.
     *
     * @param file загруженный файл
     * @return поток с содержимым .docx
     * @throws ResponseStatusException 400, если файл пустой, не .docx или недоступен
     */
    static InputStream requireDocxStream(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required and must not be empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(DOCX_EXTENSION)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .docx files are supported");
        }

        try {
            return file.getInputStream();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unable to read uploaded file: " + ex.getMessage(), ex);
        }
    }

    /**
     * Оборачивает ошибку разбора в ответ 400.
     *
     * @param ex ошибка чтения .docx
     * @return исключение для ответа 400
     */
    static ResponseStatusException invalidDocx(IOException ex) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unable to read .docx file: " + ex.getMessage(), ex);
    }
}
