package com.mortgage.reconciliation.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortgage.reconciliation.domain.Template;
import com.mortgage.reconciliation.domain.TemplateField;
import com.mortgage.reconciliation.dto.ApplicationData;
import com.mortgage.reconciliation.dto.ComparisonResult;
import com.mortgage.reconciliation.dto.ComparisonStatus;
import com.mortgage.reconciliation.dto.DocBlock;
import com.mortgage.reconciliation.dto.ExtractedField;
import com.mortgage.reconciliation.service.ComparatorService;
import com.mortgage.reconciliation.service.DocxReaderService;
import com.mortgage.reconciliation.service.NormalizationService;
import com.mortgage.reconciliation.service.RegexExtractorService;
import com.mortgage.reconciliation.service.TextNormalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Регрессионные тесты на Golden Corpus: документы из
 * {@code src/test/resources/corpus/{ddu,dkp}} прогоняются через всю цепочку
 * «чтение .docx → regex-извлечение → нормализация → сравнение с заявкой»,
 * результат сверяется с эталонным JSON рядом с документом.
 */
class AnalysisIntegrationTest {

    private static final Path CORPUS_ROOT = Path.of("src", "test", "resources", "corpus");

    /**
     * Сумма с валютой после числа: {@code 1 000 000,50 руб.}, {@code 2 500 ₽}, {@code 5000000.00}.
     * {@code (?<value>...)} — именованная группа, из которой экстрактор берёт значение;
     * {@code \d[\d\s]*} разрешает пробелы между разрядами, {@code (?:[.,]\d{1,2})?} — копейки,
     * {@code (?:руб\.?|₽|RUB)} требует валюту, иначе под шаблон попали бы сроки и номера.
     */
    private static final String MONEY_PATTERN =
            "(?<value>\\d[\\d\\s]*(?:[.,]\\d{1,2})?)\\s*(?:руб\\.?|₽|RUB)";

    /** Ставка с обязательным процентом: {@code 12,5 %}, {@code 12.5%}, {@code 9,75 % годовых}. */
    private static final String PERCENT_PATTERN = "(?<value>\\d{1,2}(?:[.,]\\d{1,4})?)\\s*%";

    /**
     * ФИО из двух-трёх слов с заглавной буквы, включая двойные фамилии через дефис
     * ({@code Сидорова-Водкина Анна Сергеевна}): каждый сегмент после дефиса начинается
     * с заглавной, поэтому класс символов разбит на части.
     */
    private static final String FIO_PATTERN =
            "(?<value>[А-ЯЁ][а-яё]*(?:-[А-ЯЁ][а-яё]*)*(?:\\s+[А-ЯЁ][а-яё]*(?:-[А-ЯЁ][а-яё]*)*){1,2})";

    /** Поля, точность извлечения которых обязана быть 100%. */
    private static final Set<String> CRITICAL_FIELDS = Set.of("loan_amount", "interest_rate");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NormalizationService normalizationService = new NormalizationService();
    private final DocxReaderService docxReaderService = new DocxReaderService(new TextNormalizer());
    private final RegexExtractorService extractorService = new RegexExtractorService();
    private final ComparatorService comparatorService = new ComparatorService(normalizationService);

    static Stream<Arguments> corpusCases() throws IOException {
        assertThat(CORPUS_ROOT).as("каталог Golden Corpus").isDirectory();

        try (Stream<Path> files = Files.walk(CORPUS_ROOT)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".docx"))
                    .sorted()
                    .map(path -> Arguments.of(path, goldenPathFor(path)))
                    .toList()
                    .stream();
        }
    }

    private static Path goldenPathFor(Path docx) {
        String name = docx.getFileName().toString();
        return docx.resolveSibling(name.substring(0, name.length() - ".docx".length()) + ".json");
    }

    @Test
    void corpusContainsDocumentsForBothDocumentTypes() throws IOException {
        try (Stream<Path> files = Files.walk(CORPUS_ROOT)) {
            Map<String, Long> byType = files
                    .filter(path -> path.getFileName().toString().endsWith(".docx"))
                    .collect(Collectors.groupingBy(
                            path -> path.getParent().getFileName().toString(),
                            LinkedHashMap::new,
                            Collectors.counting()));

            assertThat(byType.get("ddu")).as("документов DDU").isBetween(3L, 4L);
            assertThat(byType.get("dkp")).as("документов DKP").isBetween(3L, 4L);
        }
    }

    @Test
    void everyCorpusDocumentHasGoldenFile() throws IOException {
        try (Stream<Path> files = Files.walk(CORPUS_ROOT)) {
            List<String> missing = files
                    .filter(path -> path.getFileName().toString().endsWith(".docx"))
                    .filter(path -> !Files.isRegularFile(goldenPathFor(path)))
                    .map(Path::toString)
                    .toList();

            assertThat(missing).as("документы без эталонного .json").isEmpty();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("corpusCases")
    void extractsGoldenValuesAndMatchesApplication(Path docx, Path goldenFile) throws IOException {
        GoldenCase golden = objectMapper.readValue(goldenFile.toFile(), GoldenCase.class);
        Template template = templateFor(golden.docType());

        List<DocBlock> blocks = docxReaderService.readDocx(docx);
        List<ExtractedField> extracted = extractorService.extract(blocks, template);

        Map<String, String> rulesByField = template.getFields().stream()
                .collect(Collectors.toMap(
                        TemplateField::getFieldCode,
                        field -> field.getNormalizationRule() == null ? "" : field.getNormalizationRule()));

        int matched = 0;
        int criticalMatched = 0;
        int criticalTotal = 0;
        List<String> misses = new ArrayList<>();

        for (Map.Entry<String, String> expected : golden.expected().entrySet()) {
            String fieldCode = expected.getKey();
            String rule = rulesByField.getOrDefault(fieldCode, "");
            boolean critical = CRITICAL_FIELDS.contains(fieldCode);

            if (critical) {
                criticalTotal++;
            }

            boolean found = extracted.stream()
                    .filter(candidate -> fieldCode.equals(candidate.fieldCode()))
                    .anyMatch(candidate -> valuesEqual(rule, expected.getValue(), candidate.rawValue()));

            if (found) {
                matched++;
                if (critical) {
                    criticalMatched++;
                }
            } else {
                misses.add(fieldCode + ": ожидалось " + expected.getValue()
                        + ", извлечено " + rawValues(extracted, fieldCode));
            }
        }

        double precision = golden.expected().isEmpty()
                ? 1.0
                : (double) matched / golden.expected().size();
        double criticalPrecision = criticalTotal == 0
                ? 1.0
                : (double) criticalMatched / criticalTotal;

        assertThat(misses).as("пропущенные поля в %s", docx.getFileName()).isEmpty();
        assertThat(precision).as("Precision по всем полям для %s", docx.getFileName()).isEqualTo(1.0);
        assertThat(criticalPrecision).as("Precision по критическим полям для %s", docx.getFileName()).isEqualTo(1.0);

        ApplicationData appData = ApplicationData.fromJson(golden.application());
        List<ComparisonResult> comparison = comparatorService.compare(appData, extracted, template);

        assertThat(comparison).hasSameSizeAs(template.getFields());

        for (ComparisonResult result : comparison) {
            assertThat(result.status())
                    .as("%s / %s: %s", docx.getFileName(), result.fieldCode(), result.message())
                    .isEqualTo(ComparisonStatus.OK);
        }
    }

    private boolean valuesEqual(String rule, String expected, String actual) {
        Object expectedValue = normalizationService.normalize(expected, rule);
        Object actualValue = normalizationService.normalize(actual, rule);

        if (expectedValue instanceof BigDecimal expectedNumber && actualValue instanceof BigDecimal actualNumber) {
            return expectedNumber.compareTo(actualNumber) == 0;
        }

        if (expectedValue instanceof LocalDate expectedDate && actualValue instanceof LocalDate actualDate) {
            return expectedDate.equals(actualDate);
        }

        return expectedValue != null && expectedValue.equals(actualValue);
    }

    private List<String> rawValues(List<ExtractedField> extracted, String fieldCode) {
        return extracted.stream()
                .filter(field -> fieldCode.equals(field.fieldCode()))
                .map(ExtractedField::rawValue)
                .toList();
    }

    private Template templateFor(String docType) {
        String normalized = docType == null ? "" : docType.trim().toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "DDU" -> template("DDU", "Договор долевого участия",
                    field("loan_amount", "Сумма кредита", MONEY_PATTERN,
                            "сумма кредита,кредит предоставляется в размере,сумма займа", "MONEY"),
                    field("interest_rate", "Процентная ставка", PERCENT_PATTERN,
                            "процентная ставка,ставка по кредиту,годовых", "PERCENT"),
                    field("borrower_fio", "ФИО заемщика", FIO_PATTERN,
                            "заемщик,фио,фамилия имя отчество", "FIO"));
            case "DKP" -> template("DKP", "Договор купли-продажи",
                    field("loan_amount", "Цена договора", MONEY_PATTERN,
                            "цена договора,стоимость квартиры,сумма кредита", "MONEY"),
                    field("interest_rate", "Процентная ставка", PERCENT_PATTERN,
                            "процентная ставка,ставка по кредиту,годовых", "PERCENT"),
                    field("borrower_fio", "ФИО покупателя", FIO_PATTERN,
                            "покупатель,заемщик,фио", "FIO"));
            default -> throw new IllegalArgumentException("Неизвестный docType в эталоне: " + docType);
        };
    }

    private Template template(String docType, String name, TemplateField... fields) {
        Template template = new Template(docType, name);

        for (TemplateField field : fields) {
            template.addField(field);
        }

        return template;
    }

    private TemplateField field(String code, String title, String regex, String anchors, String rule) {
        TemplateField field = new TemplateField(code, title, regex);
        field.setAnchorPhrases(anchors);
        field.setNormalizationRule(rule);
        return field;
    }

    /** Эталонный результат, лежащий рядом с документом корпуса. */
    private record GoldenCase(
            String docType,
            Map<String, String> application,
            Map<String, String> expected
    ) {
    }
}
