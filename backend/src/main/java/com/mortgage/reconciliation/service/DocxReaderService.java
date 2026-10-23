package com.mortgage.reconciliation.service;

import com.mortgage.reconciliation.dto.DocBlock;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Читает .docx документ через Apache POI и возвращает плоский список блоков
 * в порядке их следования в документе: параграфы и ячейки таблиц.
 *
 * <p>Текст каждого блока проходит через {@link TextNormalizer}.
 */
@Service
public class DocxReaderService {

    private final TextNormalizer textNormalizer;

    public DocxReaderService(TextNormalizer textNormalizer) {
        this.textNormalizer = textNormalizer;
    }

    /**
     * Читает документ из файловой системы.
     *
     * @param path путь к .docx файлу
     * @return блоки документа в исходном порядке
     * @throws IOException если файл недоступен или не является корректным .docx
     */
    public List<DocBlock> readDocx(Path path) throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        try (InputStream inputStream = Files.newInputStream(path)) {
            return readDocx(inputStream);
        }
    }

    /**
     * Читает документ из потока. Поток закрывает вызывающая сторона.
     *
     * @param inputStream содержимое .docx файла
     * @return блоки документа в исходном порядке
     * @throws IOException если поток не удалось разобрать как .docx
     */
    public List<DocBlock> readDocx(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream must not be null");

        List<DocBlock> blocks = new ArrayList<>();
        int tableIndex = 0;

        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            for (IBodyElement bodyElement : document.getBodyElements()) {
                BodyElementType elementType = bodyElement.getElementType();

                if (elementType == BodyElementType.PARAGRAPH) {
                    XWPFParagraph paragraph = (XWPFParagraph) bodyElement;
                    blocks.add(DocBlock.paragraph(textNormalizer.normalize(paragraph.getText())));
                } else if (elementType == BodyElementType.TABLE) {
                    XWPFTable table = (XWPFTable) bodyElement;
                    appendTableCells(table, tableIndex, blocks);
                    tableIndex++;
                }
            }
        }

        return blocks;
    }

    private void appendTableCells(XWPFTable table, int tableIndex, List<DocBlock> blocks) {
        List<XWPFTableRow> rows = table.getRows();

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<XWPFTableCell> cells = rows.get(rowIndex).getTableCells();

            for (int colIndex = 0; colIndex < cells.size(); colIndex++) {
                String cellText = textNormalizer.normalize(cells.get(colIndex).getText());
                blocks.add(DocBlock.tableCell(tableIndex, rowIndex, colIndex, cellText));
            }
        }
    }
}
