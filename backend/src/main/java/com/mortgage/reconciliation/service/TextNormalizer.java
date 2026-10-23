package com.mortgage.reconciliation.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Приводит извлечённый из документа текст к единому виду:
 * убирает неразрывные и «узкие» пробелы, нормализует тире, кавычки и многоточие,
 * схлопывает повторяющиеся пробелы и обрезает края.
 */
@Component
public class TextNormalizer {

    private static final Pattern WHITESPACE = Pattern.compile("[\\s\\u00A0\\u2007\\u202F]+");

    private static final char[] NBSP_LIKE = {'\u00A0', '\u2007', '\u202F'};

    private static final char[] DASHES = {'\u2010', '\u2011', '\u2012', '\u2013', '\u2014', '\u2015', '\u2212'};

    private static final char[] DOUBLE_QUOTES = {'\u00AB', '\u00BB', '\u201E', '\u201C', '\u201D', '\u201F'};

    private static final char[] SINGLE_QUOTES = {'\u2018', '\u2019', '\u201A', '\u201B', '`', '\u00B4'};

    private static final char[] INVISIBLE = {'\u00AD', '\u200B', '\u200C', '\u200D', '\uFEFF'};

    /**
     * Нормализует текст блока.
     *
     * @param raw исходный текст, допускается {@code null}
     * @return очищенный текст; пустая строка для {@code null}
     */
    public String normalize(String raw) {
        if (raw == null) {
            return "";
        }

        String text = raw;

        for (char c : NBSP_LIKE) {
            text = text.replace(c, ' ');
        }
        for (char c : DASHES) {
            text = text.replace(c, '-');
        }
        for (char c : DOUBLE_QUOTES) {
            text = text.replace(c, '"');
        }
        for (char c : SINGLE_QUOTES) {
            text = text.replace(c, '\'');
        }
        for (char c : INVISIBLE) {
            text = text.replace(c, ' ');
        }

        text = text.replace("\u2026", "...");
        text = WHITESPACE.matcher(text).replaceAll(" ");

        return text.strip();
    }
}
