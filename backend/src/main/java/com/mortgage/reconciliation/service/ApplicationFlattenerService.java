package com.mortgage.reconciliation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mortgage.reconciliation.application.dto.MortgageApplicationDto;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Превращает заявку АЗС в плоскую карту «путь → строка» для компаратора.
 *
 * <p>Обходит дерево Jackson'а, а не Java-объект напрямую: так в путь попадают именно имена
 * из JSON ({@code @JsonProperty}), включая те поля, которых нет в DTO (они не сериализуются,
 * потому что DTO их не содержит).
 *
 * <p>Ключи: вложенные объекты — через точку ({@code Request.CreditApplication.RequestedLoanAmount}),
 * элементы массивов — с индексом ({@code Request.CreditApplication.PersonalApplicants[0].Surname}).
 * Значения приводятся к строке, даты — к ISO ({@code yyyy-MM-dd}), {@code null} пропускается.
 */
@Service
public class ApplicationFlattenerService {

    private final ObjectMapper objectMapper;

    /**
     * @param objectMapper Jackson для преобразования DTO в дерево; копия настроена на ISO-даты
     */
    public ApplicationFlattenerService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                // Без этого BigDecimal превращается в "5E+6": компаратору нужны обычные десятичные строки.
                .configure(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN, true);
    }

    /**
     * Сплющивает заявку в карту.
     *
     * @param application разобранная заявка; допускается {@code null}
     * @return карта «путь → значение» в порядке полей документа
     */
    public Map<String, String> flatten(MortgageApplicationDto application) {
        Map<String, String> flattened = new LinkedHashMap<>();

        if (application == null) {
            return flattened;
        }

        walk(objectMapper.valueToTree(application), "", flattened);

        return flattened;
    }

    /**
     * Рекурсивно обходит узел дерева.
     *
     * @param node  текущий узел
     * @param path  накопленный путь
     * @param target результирующая карта
     */
    private void walk(JsonNode node, String path, Map<String, String> target) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            node.fields().forEachRemaining(entry ->
                    walk(entry.getValue(), join(path, entry.getKey()), target));
            return;
        }

        if (node.isArray()) {
            int index = 0;

            for (JsonNode element : node) {
                walk(element, path + "[" + index + "]", target);
                index++;
            }

            return;
        }

        // Скалярные узлы: даты уже строки ISO, булевы — "true"/"false".
        // Для чисел берём toPlainString(): JsonNodeFactory по умолчанию нормализует BigDecimal
        // и превращает 5000000.00 в 5E+6, что неудобно компаратору.
        target.put(path, node.isNumber() ? node.decimalValue().toPlainString() : node.asText());
    }

    private String join(String path, String field) {
        return path.isEmpty() ? field : path + "." + field;
    }
}
