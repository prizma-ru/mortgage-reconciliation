package com.mortgage.reconciliation.controller;

import com.mortgage.reconciliation.dto.DocBlock;
import com.mortgage.reconciliation.service.DocxReaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
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

/**
 * Отладочный эндпоинт для проверки парсера .docx.
 *
 * <p>Доступен только в профиле {@code dev}: запускать с
 * {@code --spring.profiles.active=dev} (или {@code -Dspring-boot.run.profiles=dev}).
 */
@Tag(name = "Отладка (dev)", description = "Разбор .docx на блоки для проверки парсера; только профиль dev")
@Profile("dev")
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final DocxReaderService docxReaderService;

    /**
     * @param docxReaderService сервис разбора .docx на блоки
     */
    public DebugController(DocxReaderService docxReaderService) {
        this.docxReaderService = docxReaderService;
    }

    /**
     * Разбирает загруженный .docx и возвращает список блоков в порядке документа.
     *
     * @param file загруженный файл
     * @return блоки документа
     */
    @Operation(
            summary = "Разобрать .docx на блоки",
            description = "Возвращает параграфы и ячейки таблиц в исходном порядке — помогает "
                    + "проверить, как парсер видит документ. Доступно только в профиле dev."
    )
    @PostMapping(value = "/docx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<DocBlock> readDocx(
            @Parameter(description = "Документ в формате .docx", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(in = ParameterIn.HEADER, name = "Operation-Id", required = true,
                    description = "Идентификатор операции, например DEBUG_DOCX")
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
        try (InputStream inputStream = DocxUploadSupport.requireDocxStream(file)) {
            return docxReaderService.readDocx(inputStream);
        } catch (IOException ex) {
            throw DocxUploadSupport.invalidDocx(ex);
        }
    }
}
