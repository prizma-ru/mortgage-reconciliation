package com.mortgage.reconciliation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortgage.reconciliation.domain.Template;
import com.mortgage.reconciliation.dto.ApplicationData;
import com.mortgage.reconciliation.dto.ComparisonResult;
import com.mortgage.reconciliation.dto.DocBlock;
import com.mortgage.reconciliation.dto.ExtractedField;
import com.mortgage.reconciliation.repository.TemplateRepository;
import com.mortgage.reconciliation.service.ComparatorService;
import com.mortgage.reconciliation.service.DocxReaderService;
import com.mortgage.reconciliation.service.RegexExtractorService;
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
import java.util.Map;

/**
 * Сверка договора с заявкой: разбор .docx, извлечение полей и сравнение.
 *
 * <p>Разрешён в {@code CorsConfig} для фронтенда на {@code localhost:5173} и {@code 127.0.0.1:5173}.
 */
@RestController
@RequestMapping("/api")
public class AnalysisController {

    private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {
    };

    private final DocxReaderService docxReaderService;
    private final RegexExtractorService regexExtractorService;
    private final ComparatorService comparatorService;
    private final TemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    public AnalysisController(DocxReaderService docxReaderService,
                              RegexExtractorService regexExtractorService,
                              ComparatorService comparatorService,
                              TemplateRepository templateRepository,
                              ObjectMapper objectMapper) {
        this.docxReaderService = docxReaderService;
        this.regexExtractorService = regexExtractorService;
        this.comparatorService = comparatorService;
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Сравнивает поля договора с данными заявки.
     *
     * @param file            загруженный .docx
     * @param docType         тип документа, например {@code DDU}
     * @param applicationData JSON-объект «код поля → значение»
     * @return результат сравнения по каждому полю шаблона
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<ComparisonResult> analyze(@RequestParam("file") MultipartFile file,
                                          @RequestParam("docType") String docType,
                                          @RequestParam("applicationData") String applicationData) {
        Template template = resolveTemplate(docType);
        ApplicationData appData = parseApplicationData(applicationData);
        List<DocBlock> blocks = readBlocks(file);
        List<ExtractedField> extracted = regexExtractorService.extract(blocks, template);

        return comparatorService.compare(appData, extracted, template);
    }

    private Template resolveTemplate(String docType) {
        if (docType == null || docType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "docType is required");
        }

        String normalized = docType.trim().toUpperCase(Locale.ROOT);

        return templateRepository.findActiveByDocType(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active template for docType: " + normalized));
    }

    private ApplicationData parseApplicationData(String applicationData) {
        if (applicationData == null || applicationData.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "applicationData is required");
        }

        try {
            Map<String, Object> raw = objectMapper.readValue(applicationData, JSON_OBJECT);
            return ApplicationData.fromJson(raw);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "applicationData must be a JSON object: " + ex.getOriginalMessage(), ex);
        }
    }

    private List<DocBlock> readBlocks(MultipartFile file) {
        try (InputStream inputStream = DocxUploadSupport.requireDocxStream(file)) {
            return docxReaderService.readDocx(inputStream);
        } catch (IOException ex) {
            throw DocxUploadSupport.invalidDocx(ex);
        }
    }
}
