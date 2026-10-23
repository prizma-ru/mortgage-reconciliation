package com.mortgage.reconciliation.controller;

import com.mortgage.reconciliation.domain.Template;
import com.mortgage.reconciliation.dto.DocBlock;
import com.mortgage.reconciliation.dto.ExtractedField;
import com.mortgage.reconciliation.repository.TemplateRepository;
import com.mortgage.reconciliation.service.DocxReaderService;
import com.mortgage.reconciliation.service.RegexExtractorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

/**
 * Извлечение полей из .docx по активному шаблону типа документа.
 *
 * <p>Конвейер: {@link DocxReaderService} → {@link RegexExtractorService}.
 * Эндпоинт отладочный и доступен только в профиле {@code dev}.
 */
@Tag(name = "Извлечение (dev)", description = "Regex-извлечение полей без сравнения; только профиль dev")
@Profile("dev")
@RestController
@RequestMapping("/api/extract")
public class ExtractionController {

    private final DocxReaderService docxReaderService;
    private final RegexExtractorService regexExtractorService;
    private final TemplateRepository templateRepository;

    /**
     * @param docxReaderService     сервис разбора .docx на блоки
     * @param regexExtractorService сервис извлечения значений по правилам шаблона
     * @param templateRepository    репозиторий шаблонов документов
     */
    public ExtractionController(DocxReaderService docxReaderService,
                                RegexExtractorService regexExtractorService,
                                TemplateRepository templateRepository) {
        this.docxReaderService = docxReaderService;
        this.regexExtractorService = regexExtractorService;
        this.templateRepository = templateRepository;
    }

    /**
     * Разбирает .docx и извлекает значения полей активного шаблона.
     *
     * @param file    загруженный .docx
     * @param docType тип документа, например {@code DDU}
     * @return найденные значения; для одного поля может быть несколько записей
     */
    @Operation(
            summary = "Извлечь поля из .docx",
            description = "Прогоняет документ через парсер и regex-экстрактор активного шаблона, "
                    + "не сравнивая значения с заявкой и не сохраняя результат. Только профиль dev."
    )
    @PostMapping(value = "/docx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<ExtractedField> extract(
            @Parameter(description = "Договор в формате .docx", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Тип документа: DDU или DKP", example = "DDU", required = true)
            @RequestParam("docType") String docType,
            @Parameter(in = ParameterIn.HEADER, name = "Operation-Id", required = true,
                    description = "Идентификатор операции, например EXTRACT_DOCX")
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
        if (docType == null || docType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "docType is required");
        }

        String normalizedDocType = docType.trim().toUpperCase(Locale.ROOT);
        Template template = templateRepository.findActiveByDocType(normalizedDocType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active template for docType: " + normalizedDocType));

        List<DocBlock> blocks;
        try (InputStream inputStream = DocxUploadSupport.requireDocxStream(file)) {
            blocks = docxReaderService.readDocx(inputStream);
        } catch (IOException ex) {
            throw DocxUploadSupport.invalidDocx(ex);
        }

        return regexExtractorService.extract(blocks, template);
    }
}
