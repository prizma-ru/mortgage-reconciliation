package com.mortgage.reconciliation.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Month;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Приводит сырые значения, извлечённые из договора или пришедшие из заявки,
 * к каноническому виду согласно правилу нормализации поля шаблона.
 *
 * <p>Поддерживаемые правила: {@code MONEY}, {@code PERCENT}, {@code FIO}, {@code DATE}.
 * Неизвестное правило или {@code null} означают простую очистку пробелов.
 */
@Service
public class NormalizationService {

    /** Пробелы всех видов, включая неразрывные: в договорах ими разделяют разряды суммы. */
    private static final Pattern WHITESPACE = Pattern.compile("[\\s\\u00A0\\u2007\\u202F]+");

    /** Обозначения рубля после суммы; удаляются до разбора числа. */
    private static final Pattern CURRENCY = Pattern.compile("(?iu)руб\\.?|рублей|рубля|₽|rub");

    /** Всё, кроме цифр, разделителей и минуса: буквенные пояснения вроде «годовых». */
    private static final Pattern NOT_NUMBER = Pattern.compile("[^0-9.,-]");

    /** Дата вида {@code 15.01.2025}. */
    private static final Pattern NUMERIC_DATE = Pattern.compile("(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})");

    /** Дата вида {@code 15 января 2025}: день, русский месяц в родительном падеже, год. */
    private static final Pattern TEXT_DATE = Pattern.compile("(\\d{1,2})\\s+([а-яё]+)\\s+(\\d{4})", Pattern.CASE_INSENSITIVE);

    /** Родительный падеж обязателен: в договорах пишут «15 января», а не «15 январь». */
    private static final Map<String, Month> RU_MONTHS = Map.ofEntries(
            Map.entry("января", Month.JANUARY),
            Map.entry("февраля", Month.FEBRUARY),
            Map.entry("марта", Month.MARCH),
            Map.entry("апреля", Month.APRIL),
            Map.entry("мая", Month.MAY),
            Map.entry("июня", Month.JUNE),
            Map.entry("июля", Month.JULY),
            Map.entry("августа", Month.AUGUST),
            Map.entry("сентября", Month.SEPTEMBER),
            Map.entry("октября", Month.OCTOBER),
            Map.entry("ноября", Month.NOVEMBER),
            Map.entry("декабря", Month.DECEMBER)
    );

    /**
     * Нормализует значение по правилу поля шаблона.
     *
     * @param rawValue          сырое значение
     * @param normalizationRule правило ({@code MONEY}, {@code PERCENT}, {@code FIO}, {@code DATE})
     * @return {@link BigDecimal} для MONEY/PERCENT, {@link LocalDate} для DATE, {@link String} для FIO
     *         и простой очистки; {@code null}, если значение пустое или не разбирается
     */
    public Object normalize(String rawValue, String normalizationRule) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String rule = normalizationRule == null
                ? ""
                : normalizationRule.trim().toUpperCase(Locale.ROOT);

        return switch (rule) {
            case "MONEY" -> normalizeMoney(rawValue);
            case "PERCENT" -> normalizePercent(rawValue);
            case "FIO" -> normalizeFio(rawValue);
            case "DATE" -> normalizeDate(rawValue);
            default -> normalizePlain(rawValue);
        };
    }

    /**
     * Приводит денежную сумму к числу: убирает пробелы и валюту, запятую меняет на точку.
     *
     * @param rawValue например {@code "1 000 000,50 руб."}
     * @return {@code 1000000.50} или {@code null}, если разобрать не удалось
     */
    public BigDecimal normalizeMoney(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String cleaned = CURRENCY.matcher(rawValue).replaceAll(" ");
        return parseDecimal(cleaned);
    }

    /**
     * Приводит ставку к числу: убирает знак процента, пробелы и текстовые пояснения.
     *
     * @param rawValue например {@code "6,5 % годовых"}
     * @return {@code 6.5} или {@code null}, если разобрать не удалось
     */
    public BigDecimal normalizePercent(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String cleaned = rawValue.replace('%', ' ');
        return parseDecimal(cleaned);
    }

    /**
     * Очищает ФИО: схлопывает пробелы и приводит каждое слово к виду «С заглавной».
     *
     * @param rawValue например {@code "  иванов   иван   иванович "}
     * @return {@code "Иванов Иван Иванович"} или {@code null} для пустой строки
     */
    public String normalizeFio(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String cleaned = WHITESPACE.matcher(rawValue).replaceAll(" ").strip();

        if (cleaned.isEmpty()) {
            return null;
        }

        StringBuilder result = new StringBuilder(cleaned.length());
        boolean capitalizeNext = true;

        for (char symbol : cleaned.toCharArray()) {
            if (Character.isLetter(symbol)) {
                result.append(capitalizeNext ? Character.toUpperCase(symbol) : Character.toLowerCase(symbol));
                capitalizeNext = false;
            } else {
                result.append(symbol);
                capitalizeNext = symbol == ' ' || symbol == '-';
            }
        }

        return result.toString();
    }

    /**
     * Разбирает дату в формате {@code dd.MM.yyyy} или {@code dd MMMM yyyy} с русским месяцем.
     *
     * @param rawValue например {@code "15.01.2025"} или {@code "15 января 2025 г."}
     * @return дата или {@code null}, если формат не распознан
     */
    public LocalDate normalizeDate(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String cleaned = WHITESPACE.matcher(rawValue).replaceAll(" ").strip();
        cleaned = cleaned.replace("г.", " ").strip();

        Matcher numeric = NUMERIC_DATE.matcher(cleaned);
        if (numeric.matches()) {
            return toDate(Integer.parseInt(numeric.group(1)),
                    Integer.parseInt(numeric.group(2)),
                    Integer.parseInt(numeric.group(3)));
        }

        Matcher textual = TEXT_DATE.matcher(cleaned);
        if (textual.matches()) {
            Month month = RU_MONTHS.get(textual.group(2).toLowerCase(Locale.ROOT));

            if (month == null) {
                return null;
            }

            return toDate(Integer.parseInt(textual.group(1)),
                    month.getValue(),
                    Integer.parseInt(textual.group(3)));
        }

        return null;
    }

    private String normalizePlain(String rawValue) {
        return WHITESPACE.matcher(rawValue).replaceAll(" ").strip();
    }

    private LocalDate toDate(int day, int month, int year) {
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException ex) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String rawValue) {
        String cleaned = NOT_NUMBER.matcher(WHITESPACE.matcher(rawValue).replaceAll("")).replaceAll("");

        if (cleaned.isEmpty() || cleaned.equals("-")) {
            return null;
        }

        String normalized = cleaned.replace(',', '.');

        try {
            return new BigDecimal(normalizeGrouping(normalized));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Разбирается с разделителями тысяч: {@code 1.000.000} — это миллион,
     * а {@code 1000000.50} — число с копейками.
     */
    private String normalizeGrouping(String value) {
        String[] groups = value.split("\\.", -1);

        if (groups.length < 2) {
            return value;
        }

        boolean thousandsOnly = true;

        for (int index = 1; index < groups.length; index++) {
            if (groups[index].length() != 3) {
                thousandsOnly = false;
                break;
            }
        }

        if (thousandsOnly) {
            return String.join("", groups);
        }

        String integerPart = String.join("", java.util.Arrays.copyOf(groups, groups.length - 1));
        String fractionPart = groups[groups.length - 1];

        return fractionPart.isEmpty() ? integerPart : integerPart + "." + fractionPart;
    }
}
