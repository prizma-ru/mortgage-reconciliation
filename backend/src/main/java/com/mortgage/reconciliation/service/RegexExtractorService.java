package com.mortgage.reconciliation.service;

import com.mortgage.reconciliation.domain.Template;
import com.mortgage.reconciliation.domain.TemplateField;
import com.mortgage.reconciliation.dto.BlockType;
import com.mortgage.reconciliation.dto.DocBlock;
import com.mortgage.reconciliation.dto.ExtractedField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Извлекает значения полей шаблона из блоков документа по правилам из базы данных.
 *
 * <p>Алгоритм для каждого блока:
 * <ol>
 *   <li>ищем якорную фразу; если она есть — применяем регулярное выражение к тексту
 *       сразу после неё (не более двух предложений);</li>
 *   <li>если якоря в блоке нет — применяем выражение ко всему тексту блока.</li>
 * </ol>
 *
 * <p>Разные значения одного поля попадают в результат отдельными записями
 * (их можно помечать как требующие ручной проверки), одинаковые — схлопываются
 * с сохранением максимальной уверенности.
 */
@Service
public class RegexExtractorService {

    private static final Logger log = LoggerFactory.getLogger(RegexExtractorService.class);

    /** Уверенность, когда значение найдено рядом с якорной фразой. */
    private static final double ANCHOR_CONFIDENCE = 0.9;

    /** Уверенность, когда якоря нет и выражение применено ко всему блоку. */
    private static final double BLOCK_CONFIDENCE = 0.4;

    private static final int MAX_SENTENCES_AFTER_ANCHOR = 2;

    private static final int MAX_WINDOW_LENGTH = 400;

    private static final String VALUE_GROUP = "value";

    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+");

    private static final Pattern LEADING_SEPARATORS = Pattern.compile("^[\\s:;,—–-]+");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * Извлекает значения всех полей шаблона.
     *
     * @param blocks   блоки документа в исходном порядке
     * @param template шаблон с полями
     * @return найденные значения; пустой список, если ничего не найдено
     */
    public List<ExtractedField> extract(List<DocBlock> blocks, Template template) {
        List<ExtractedField> extracted = new ArrayList<>();

        if (blocks == null || blocks.isEmpty() || template == null) {
            return extracted;
        }

        for (TemplateField field : template.getFields()) {
            Pattern pattern = compile(field);

            if (pattern == null) {
                continue;
            }

            List<String> anchors = splitAnchors(field.getAnchorPhrases());
            Map<String, ExtractedField> values = new LinkedHashMap<>();

            for (int index = 0; index < blocks.size(); index++) {
                DocBlock block = blocks.get(index);
                String text = block == null ? null : block.text();

                if (text == null || text.isBlank()) {
                    continue;
                }

                String location = describeLocation(block, index);
                String anchoredText = windowAfterAnchor(text, anchors);

                if (anchoredText != null) {
                    collect(pattern, anchoredText, field, location, ANCHOR_CONFIDENCE, values);
                } else {
                    collect(pattern, text, field, location, BLOCK_CONFIDENCE, values);
                }
            }

            extracted.addAll(values.values());
        }

        return extracted;
    }

    private Pattern compile(TemplateField field) {
        String regex = field.getRegexPattern();

        if (regex == null || regex.isBlank()) {
            log.warn("Template field '{}' has no regex pattern, skipping", field.getFieldCode());
            return null;
        }

        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException ex) {
            log.warn("Template field '{}' has invalid regex '{}': {}",
                    field.getFieldCode(), regex, ex.getDescription());
            return null;
        }
    }

    private void collect(Pattern pattern,
                         String text,
                         TemplateField field,
                         String location,
                         double confidence,
                         Map<String, ExtractedField> values) {
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String raw = collapseWhitespace(groupOrWholeMatch(matcher));

            if (raw.isEmpty()) {
                continue;
            }

            ExtractedField candidate = new ExtractedField(field.getFieldCode(), raw, location, confidence);
            values.merge(raw, candidate, (existing, next) ->
                    existing.confidence() >= next.confidence() ? existing : next);
        }
    }

    private String groupOrWholeMatch(Matcher matcher) {
        try {
            String value = matcher.group(VALUE_GROUP);
            return value != null ? value : matcher.group();
        } catch (IllegalArgumentException ex) {
            return matcher.group();
        }
    }

    private String collapseWhitespace(String value) {
        return value == null ? "" : WHITESPACE.matcher(value).replaceAll(" ").strip();
    }

    private List<String> splitAnchors(String anchorPhrases) {
        List<String> anchors = new ArrayList<>();

        if (anchorPhrases == null || anchorPhrases.isBlank()) {
            return anchors;
        }

        for (String phrase : anchorPhrases.split(",")) {
            String normalized = collapseWhitespace(phrase).toLowerCase(Locale.ROOT);

            if (!normalized.isEmpty()) {
                anchors.add(normalized);
            }
        }

        return anchors;
    }

    /**
     * Возвращает окно текста после первой найденной якорной фразы или {@code null},
     * если ни один якорь в блоке не встретился.
     */
    private String windowAfterAnchor(String text, List<String> anchors) {
        if (anchors.isEmpty()) {
            return null;
        }

        String haystack = text.toLowerCase(Locale.ROOT);
        int anchorEnd = -1;

        for (String anchor : anchors) {
            int position = haystack.indexOf(anchor);

            if (position < 0) {
                continue;
            }

            int candidateEnd = position + anchor.length();

            if (anchorEnd < 0 || position < anchorEnd) {
                anchorEnd = candidateEnd;
            }
        }

        if (anchorEnd < 0) {
            return null;
        }

        String tail = LEADING_SEPARATORS.matcher(text.substring(anchorEnd)).replaceFirst("");
        String window = takeSentences(tail, MAX_SENTENCES_AFTER_ANCHOR);

        return window.length() > MAX_WINDOW_LENGTH ? window.substring(0, MAX_WINDOW_LENGTH) : window;
    }

    private String takeSentences(String text, int maxSentences) {
        Matcher matcher = SENTENCE_END.matcher(text);
        int seen = 0;

        while (matcher.find()) {
            seen++;

            if (seen >= maxSentences) {
                return text.substring(0, matcher.start());
            }
        }

        return text;
    }

    private String describeLocation(DocBlock block, int index) {
        if (block.type() == BlockType.TABLE_CELL) {
            return "table %d, row %d, cell %d".formatted(
                    block.tableIndex(), block.rowIndex(), block.colIndex());
        }

        return "paragraph " + (index + 1);
    }
}
