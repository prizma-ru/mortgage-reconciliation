package com.mortgage.reconciliation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mortgage.reconciliation.application.dto.MortgageApplicationDto;
import com.mortgage.reconciliation.exception.ApplicationParsingException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Разбирает JSON-заявку АЗС в DTO и отдаёт её плоское представление.
 *
 * <p>Реальная заявка содержит больше сотни атрибутов, из которых DTO описывает ключевые,
 * поэтому неизвестные поля игнорируются ({@code FAIL_ON_UNKNOWN_PROPERTIES=false}).
 *
 * <p>Ранее в контроллере использовался плоский {@code Map<String, String>}. Чтобы не сломать
 * существующие сценарии (UI и тесты присылают плоский JSON), поддерживаются оба формата:
 * структурированная заявка с блоком {@code Request} разбирается в DTO и сплющивается,
 * а плоский JSON превращается в карту как есть.
 */
@Service
public class ApplicationParserService {

    private final ObjectMapper objectMapper;
    private final ApplicationFlattenerService flattenerService;

    /**
     * @param objectMapper     Jackson из Spring Boot (с поддержкой {@code java.time})
     * @param flattenerService сервис превращения DTO в плоскую карту
     */
    public ApplicationParserService(ObjectMapper objectMapper, ApplicationFlattenerService flattenerService) {
        this.objectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.flattenerService = flattenerService;
    }

    /**
     * Разбирает JSON заявки в DTO.
     *
     * @param jsonString JSON-объект заявки
     * @return заявка с ключевыми полями; отсутствующие блоки будут {@code null}
     * @throws ApplicationParsingException если строка пустая, не JSON или не соответствует структуре
     */
    public MortgageApplicationDto parse(String jsonString) {
        JsonNode root = readTree(jsonString);

        try {
            return objectMapper.treeToValue(root, MortgageApplicationDto.class);
        } catch (JsonProcessingException ex) {
            throw new ApplicationParsingException(
                    "Не удалось разобрать заявку: " + ex.getOriginalMessage(), ex);
        }
    }

    /**
     * Разбирает заявку и возвращает её плоское представление.
     *
     * @param jsonString JSON-объект заявки
     * @return карта «путь → значение»; для плоского JSON — исходные ключи
     * @throws ApplicationParsingException если строка пустая или не JSON-объект
     */
    public Map<String, String> parseToFlatMap(String jsonString) {
        JsonNode root = readTree(jsonString);

        if (isStructuredApplication(root)) {
            return flattenerService.flatten(parse(jsonString));
        }

        // Старый плоский формат заявки: {"loan_amount": "1000000", ...}
        Map<String, String> flat = new LinkedHashMap<>();
        root.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();

            if (value.isValueNode() && !value.isNull()) {
                flat.put(entry.getKey(), value.asText());
            }
        });

        return flat;
    }

    private boolean isStructuredApplication(JsonNode root) {
        return root.has("Request") || root.has("SystemID");
    }

    private JsonNode readTree(String jsonString) {
        if (jsonString == null || jsonString.isBlank()) {
            throw new ApplicationParsingException("Заявка пустая: ожидается JSON-объект");
        }

        JsonNode root;

        try {
            root = objectMapper.readTree(jsonString);
        } catch (JsonProcessingException ex) {
            throw new ApplicationParsingException(
                    "Заявка не является корректным JSON: " + ex.getOriginalMessage(), ex);
        }

        if (root == null || !root.isObject()) {
            throw new ApplicationParsingException("Заявка должна быть JSON-объектом");
        }

        return root;
    }
}
