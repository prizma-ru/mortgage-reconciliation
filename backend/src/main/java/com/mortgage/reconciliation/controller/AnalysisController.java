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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
@Tag(name = "Анализ документов", description = "Сверка договора с заявкой и история проверок")
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

    /**
     * @param docxReaderService     сервис разбора .docx на блоки
     * @param regexExtractorService сервис извлечения значений по правилам шаблона
     * @param comparatorService     сервис сверки значений с заявкой
     * @param persistenceService    сервис сохранения и чтения истории запусков
     * @param templateRepository    репозиторий шаблонов документов
     * @param objectMapper          Jackson для разбора JSON-заявки
     */
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
    @Operation(
            summary = "Сверить договор с заявкой",
            description = "Разбирает .docx, извлекает поля по активному шаблону docType, нормализует "
                    + "значения, сравнивает их с JSON-заявкой и сохраняет протокол в БД."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisRunResponse analyze(
            @Parameter(description = "Договор в формате .docx", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "JSON-объект «код поля → значение», например "
                    + "{\"loan_amount\":\"1 000 000,50\"}", required = true)
            @RequestParam("application") String application,
            @Parameter(description = "Тип документа: DDU или DKP", example = "DDU", required = true)
            @RequestParam("docType") String docType,
            @Parameter(in = ParameterIn.HEADER, name = "Operation-Id", required = true,
                    description = "Идентификатор операции, например ANALYZE_CONTRACT")
            @RequestHeader(name = "Operation-Id", required = true) String operationId,
            @Parameter(in = ParameterIn.HEADER, name = "x-call-id", required = true,
                    description = "Сквозной идентификатор запроса (UUID v7)")
            @RequestHeader(name = "x-call-id", required = true) String callId,
            @Parameter(in = ParameterIn.HEADER, name = "x-mdm-id",
                    description = "Идентификатор клиента (опционально)")
            @RequestHeader(name = "x-mdm-id", required = false) String mdmId,
            @Parameter(in = ParameterIn.HEADER, name = "x-user-session-id",
                    description = "Идентификатор сессии пользователя (опционально)")
            @RequestHeader(name = "x-user-session-id", required = false) String userSessionId,
            @Parameter(in = ParameterIn.HEADER, name = "X-Segment-Key",
                    description = "Ключ маршрутизации, например номер заявки (опционально)")
            @RequestHeader(name = "X-Segment-Key", required = false) String segmentKey) {
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
    @Operation(
            summary = "История запусков анализа",
            description = "Постраничный список проверок, сначала новые. Без вложенных результатов: "
                    + "для деталей используйте GET /api/analyses/{id}."
    )
    @GetMapping
    public AnalysisRunPageResponse findAll(
            @Parameter(description = "Номер страницы с нуля", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Размер страницы, не больше 100", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size,
            @Parameter(in = ParameterIn.HEADER, name = "Operation-Id", required = true,
                    description = "Идентификатор операции, например GET_ANALYSES")
            @RequestHeader(name = "Operation-Id", required = true) String operationId,
            @Parameter(in = ParameterIn.HEADER, name = "x-call-id", required = true,
                    description = "Сквозной идентификатор запроса (UUID v7)")
            @RequestHeader(name = "x-call-id", required = true) String callId,
            @Parameter(in = ParameterIn.HEADER, name = "x-mdm-id",
                    description = "Идентификатор клиента (опционально)")
            @RequestHeader(name = "x-mdm-id", required = false) String mdmId,
            @Parameter(in = ParameterIn.HEADER, name = "x-user-session-id",
                    description = "Идентификатор сессии пользователя (опционально)")
            @RequestHeader(name = "x-user-session-id", required = false) String userSessionId,
            @Parameter(in = ParameterIn.HEADER, name = "X-Segment-Key",
                    description = "Ключ маршрутизации, например номер заявки (опционально)")
            @RequestHeader(name = "X-Segment-Key", required = false) String segmentKey) {
        return AnalysisRunPageResponse.from(persistenceService.findRuns(page, size));
    }

    /**
     * Возвращает сохранённый результат запуска.
     *
     * @param id идентификатор запуска
     * @return запуск с результатами по полям
     */
    @Operation(
            summary = "Результат запуска по id",
            description = "Возвращает сохранённый протокол сверки вместе с результатами по полям. "
                    + "Если запуск не найден, возвращается 404."
    )
    @GetMapping("/{id}")
    public AnalysisRunResponse findById(
            @Parameter(description = "Идентификатор запуска", example = "1", required = true)
            @PathVariable("id") Long id,
            @Parameter(in = ParameterIn.HEADER, name = "Operation-Id", required = true,
                    description = "Идентификатор операции, например GET_ANALYSIS")
            @RequestHeader(name = "Operation-Id", required = true) String operationId,
            @Parameter(in = ParameterIn.HEADER, name = "x-call-id", required = true,
                    description = "Сквозной идентификатор запроса (UUID v7)")
            @RequestHeader(name = "x-call-id", required = true) String callId,
            @Parameter(in = ParameterIn.HEADER, name = "x-mdm-id",
                    description = "Идентификатор клиента (опционально)")
            @RequestHeader(name = "x-mdm-id", required = false) String mdmId,
            @Parameter(in = ParameterIn.HEADER, name = "x-user-session-id",
                    description = "Идентификатор сессии пользователя (опционально)")
            @RequestHeader(name = "x-user-session-id", required = false) String userSessionId,
            @Parameter(in = ParameterIn.HEADER, name = "X-Segment-Key",
                    description = "Ключ маршрутизации, например номер заявки (опционально)")
            @RequestHeader(name = "X-Segment-Key", required = false) String segmentKey) {
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
