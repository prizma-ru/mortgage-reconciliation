package com.mortgage.reconciliation.service;

import com.mortgage.reconciliation.domain.Template;
import com.mortgage.reconciliation.domain.TemplateField;
import com.mortgage.reconciliation.dto.ApplicationData;
import com.mortgage.reconciliation.dto.ComparisonResult;
import com.mortgage.reconciliation.dto.ComparisonStatus;
import com.mortgage.reconciliation.dto.ExtractedField;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Сверяет значения договора с заявкой и присваивает каждому полю статус.
 *
 * <p>Правила: нет значения в договоре — {@link ComparisonStatus#MISSING};
 * нет в заявке — {@link ComparisonStatus#WARN}; несколько разных значений в договоре —
 * {@link ComparisonStatus#MANUAL}; иначе значения нормализуются и сравниваются
 * ({@link ComparisonStatus#OK} или {@link ComparisonStatus#ERROR}).
 */
@Service
public class ComparatorService {

    /** Порог похожести для нечёткого сравнения ФИО. */
    private static final double FIO_SIMILARITY_THRESHOLD = 0.85;

    private static final String RULE_FIO = "FIO";

    private final NormalizationService normalizationService;

    public ComparatorService(NormalizationService normalizationService) {
        this.normalizationService = normalizationService;
    }

    /**
     * Сравнивает данные заявки с извлечёнными значениями договора по всем полям шаблона.
     *
     * @param appData      значения заявки
     * @param contractData значения, извлечённые из договора
     * @param template     шаблон с полями
     * @return результат по каждому полю в порядке шаблона
     */
    public List<ComparisonResult> compare(ApplicationData appData,
                                          List<ExtractedField> contractData,
                                          Template template) {
        Map<String, List<ExtractedField>> byFieldCode = groupByFieldCode(contractData);
        List<ComparisonResult> results = new ArrayList<>();

        for (TemplateField field : template.getFields()) {
            String fieldCode = field.getFieldCode();
            List<ExtractedField> contractValues = byFieldCode.getOrDefault(fieldCode, List.of());
            String appRaw = appData == null ? null : appData.find(fieldCode).orElse(null);

            results.add(compareField(field, appRaw, contractValues));
        }

        return results;
    }

    private Map<String, List<ExtractedField>> groupByFieldCode(List<ExtractedField> contractData) {
        Map<String, List<ExtractedField>> grouped = new LinkedHashMap<>();

        if (contractData != null) {
            for (ExtractedField field : contractData) {
                if (field != null && field.fieldCode() != null) {
                    grouped.computeIfAbsent(field.fieldCode(), code -> new ArrayList<>()).add(field);
                }
            }
        }

        return grouped;
    }

    private ComparisonResult compareField(TemplateField field,
                                          String appRaw,
                                          List<ExtractedField> contractValues) {
        String fieldCode = field.getFieldCode();
        String title = field.getTitle();
        String rule = field.getNormalizationRule();

        String appDisplay = normalizeDisplay(appRaw, rule);
        List<String> distinctContractValues = distinctRawValues(contractValues);

        if (distinctContractValues.isEmpty()) {
            return new ComparisonResult(fieldCode, title, appDisplay, null,
                    ComparisonStatus.MISSING, "Поле не найдено в договоре");
        }

        String contractDisplay = normalizeDisplay(distinctContractValues.get(0), rule);

        if (appRaw == null || appRaw.isBlank()) {
            return new ComparisonResult(fieldCode, title, null, contractDisplay,
                    ComparisonStatus.WARN, "Поле есть в договоре, но отсутствует в заявке");
        }

        if (distinctContractValues.size() > 1) {
            String joined = String.join(" | ", distinctContractValues.stream()
                    .map(value -> normalizeDisplay(value, rule))
                    .toList());

            return new ComparisonResult(fieldCode, title, appDisplay, joined,
                    ComparisonStatus.MANUAL,
                    "В договоре найдено несколько разных значений поля — требуется ручная проверка");
        }

        Object appValue = normalizationService.normalize(appRaw, rule);
        Object contractValue = normalizationService.normalize(distinctContractValues.get(0), rule);

        if (appValue == null || contractValue == null) {
            return new ComparisonResult(fieldCode, title, appDisplay, contractDisplay,
                    ComparisonStatus.MANUAL,
                    "Не удалось нормализовать значение — требуется ручная проверка");
        }

        if (valuesMatch(rule, appValue, contractValue)) {
            String message = RULE_FIO.equalsIgnoreCase(rule)
                    ? "Значения совпадают (ФИО сравнено с учётом регистра и пробелов)"
                    : "Значения совпадают";

            return new ComparisonResult(fieldCode, title,
                    canonicalText(appValue), canonicalText(contractValue),
                    ComparisonStatus.OK, message);
        }

        return new ComparisonResult(fieldCode, title,
                canonicalText(appValue), canonicalText(contractValue),
                ComparisonStatus.ERROR,
                "Значения различаются: заявка = " + canonicalText(appValue)
                        + ", договор = " + canonicalText(contractValue));
    }

    private List<String> distinctRawValues(List<ExtractedField> contractValues) {
        return contractValues.stream()
                .map(ExtractedField::rawValue)
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .map(String::strip)
                .distinct()
                .toList();
    }

    private boolean valuesMatch(String rule, Object appValue, Object contractValue) {
        if (appValue instanceof BigDecimal appNumber && contractValue instanceof BigDecimal contractNumber) {
            return appNumber.compareTo(contractNumber) == 0;
        }

        if (appValue instanceof LocalDate appDate && contractValue instanceof LocalDate contractDate) {
            return appDate.equals(contractDate);
        }

        String appText = canonicalText(appValue);
        String contractText = canonicalText(contractValue);

        if (RULE_FIO.equalsIgnoreCase(rule)) {
            return similarity(appText, contractText) >= FIO_SIMILARITY_THRESHOLD;
        }

        return appText.equalsIgnoreCase(contractText);
    }

    private String normalizeDisplay(String rawValue, String rule) {
        Object normalized = normalizationService.normalize(rawValue, rule);
        return normalized == null ? null : canonicalText(normalized);
    }

    private String canonicalText(Object value) {
        if (value instanceof BigDecimal number) {
            return number.stripTrailingZeros().toPlainString();
        }

        return value.toString();
    }

    /**
     * Похожесть строк: {@code 1 - расстояние Левенштейна / длина большей строки}.
     */
    private double similarity(String left, String right) {
        String first = left.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
        String second = right.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();

        if (first.equals(second)) {
            return 1.0;
        }

        int longest = Math.max(first.length(), second.length());

        if (longest == 0) {
            return 1.0;
        }

        return 1.0 - (double) levenshteinDistance(first, second) / longest;
    }

    private int levenshteinDistance(String first, String second) {
        int[] previous = new int[second.length() + 1];
        int[] current = new int[second.length() + 1];

        for (int column = 0; column <= second.length(); column++) {
            previous[column] = column;
        }

        for (int row = 1; row <= first.length(); row++) {
            current[0] = row;

            for (int column = 1; column <= second.length(); column++) {
                int substitutionCost = first.charAt(row - 1) == second.charAt(column - 1) ? 0 : 1;

                current[column] = Math.min(
                        Math.min(current[column - 1] + 1, previous[column] + 1),
                        previous[column - 1] + substitutionCost);
            }

            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[second.length()];
    }
}
