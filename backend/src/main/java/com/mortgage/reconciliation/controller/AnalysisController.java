package com.mortgage.reconciliation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortgage.reconciliation.domain.AnalysisRun;
import com.mortgage.reconciliation.domain.Template;
import com.mortgage.reconciliation.dto.AnalysisRunPageResponse;
import com.mortgage.reconciliation.dto.AnalysisRunResponse;
import com.mortgage.reconciliation.dto.ApplicationData;
import com.mortgage.reconciliation.dto.ComparisonResult;
import com.mortgage.reconciliation.dto.DocBlock;
import com.mortgage.reconciliation.dto.ExtractedField;
import com.mortgage.reconciliation.repository.TemplateRepository;
import com.mortgage.reconciliation.service.AnalysisPersistenceService;
import com.mortgage.reconciliation.service.ComparatorService;
import com.mortgage.reconciliation.service.DocxReaderService;
import com.mortgage.reconciliation.service.RegexExtractorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import java.util.NoSuchElementException;

/**
 * Сверка договора с заявкой и история проверок.
 *
 * <p>Разрешён в {@code CorsConfig} для фронтенда на {@code localhost:5173} и {@code 127.0.0.1:5173}.
 */
@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {
    };

    private static final int MAX_FILE_NAME_LENGTH = 255;

    private final DocxReaderService docxReaderService;
    private final RegexExtractorService regexExtractorService;
    private final ComparatorService comparatorService;
    private final AnalysisPersistenceService persistenceService;
    private final TemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    public AnalysisController(DocxReaderService docxReaderService,
                              RegexExtractorService regexExtractorService,
                              ComparatorService comparatorService,
                              AnalysisPersistenceService persistenceService,
                              TemplateRepository templateRepository,
                              ObjectMapper objectMapper) {
        this.docxReaderService = docxReaderService;
        this.regexExtractorService = regexExtractorService;
        this.comparatorService = comparatorService;
        this.persistenceService = persistenceService;
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Разбирает договор, сравнивает его с заявкой и сохраняет результат в БД.
     *
     * @param file        загруженный .docx
     * @param application JSON-объект «код поля → значение»
     * @param docType     тип документа, например {@code DDU}
     * @return сохранённый запуск с результатами по полям
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisRunResponse analyze(@RequestParam("file") MultipartFile file,
                                       @RequestParam("application") String application,
                                       @RequestParam("docType") String docType) {
        Template template = resolveTemplate(docType);
        ApplicationData appData = parseApplication(application);
        List<DocBlock> blocks = readBlocks(file);
        List<ExtractedField> extracted = regexExtractorService.extract(blocks, template);
        List<ComparisonResult> comparison = comparatorService.compare(appData, extracted, template);

        AnalysisRun run = persistenceService.save(comparison, fileNameOf(file), template.getDocType());

        return AnalysisRunResponse.from(run);
    }

    /**
     * Возвращает страницу истории запусков (сначала новые).
     *
     * @param page номер страницы с нуля
     * @param size размер страницы (не больше 100)
     * @return страница с краткой информацией о запусках
     */
    @GetMapping
    public AnalysisRunPageResponse findAll(@RequestParam(name = "page", defaultValue = "0") int page,
                                           @RequestParam(name = "size", defaultValue = "20") int size) {
        return AnalysisRunPageResponse.from(persistenceService.findRuns(page, size));
    }

    /**
     * Возвращает сохранённый результат запуска.
     *
     * @param id идентификатор запуска
     * @return запуск с результатами по полям
     */
    @GetMapping("/{id}")
    public AnalysisRunResponse findById(@PathVariable("id") Long id) {
        try {
            return AnalysisRunResponse.from(persistenceService.findRun(id));
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
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

    private ApplicationData parseApplication(String application) {
        if (application == null || application.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "application is required");
        }

        try {
            Map<String, Object> raw = objectMapper.readValue(application, JSON_OBJECT);
            return ApplicationData.fromJson(raw);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "application must be a JSON object: " + ex.getOriginalMessage(), ex);
        }
    }

    private List<DocBlock> readBlocks(MultipartFile file) {
        try (InputStream inputStream = DocxUploadSupport.requireDocxStream(file)) {
            return docxReaderService.readDocx(inputStream);
        } catch (IOException ex) {
            throw DocxUploadSupport.invalidDocx(ex);
        }
    }

    private String fileNameOf(MultipartFile file) {
        String fileName = file.getOriginalFilename();

        if (fileName == null || fileName.isBlank()) {
            return "unknown.docx";
        }

        String trimmed = fileName.strip();
        return trimmed.length() > MAX_FILE_NAME_LENGTH ? trimmed.substring(0, MAX_FILE_NAME_LENGTH) : trimmed;
    }
}
