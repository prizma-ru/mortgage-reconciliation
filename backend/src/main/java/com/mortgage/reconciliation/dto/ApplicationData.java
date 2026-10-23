package com.mortgage.reconciliation.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Данные заявки: значения полей по коду ({@code fieldCode} → сырая строка).
 */
public record ApplicationData(Map<String, String> values) {

    public ApplicationData {
        values = values == null ? Map.of() : Map.copyOf(values);
    }

    /**
     * Создаёт заявку из разобранного JSON. Числа и булевы значения приводятся к строке,
     * {@code null} пропускаются.
     *
     * @param raw разобранный JSON-объект
     * @return данные заявки
     */
    public static ApplicationData fromJson(Map<String, ?> raw) {
        Map<String, String> converted = new LinkedHashMap<>();

        if (raw != null) {
            raw.forEach((key, value) -> {
                if (key != null && value != null) {
                    converted.put(key, value.toString());
                }
            });
        }

        return new ApplicationData(converted);
    }

    /**
     * Ищет значение поля заявки по коду.
     *
     * @param fieldCode код поля, например {@code loan_amount}
     * @return значение, если оно есть и не пустое
     */
    public Optional<String> find(String fieldCode) {
        String value = values.get(fieldCode);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
