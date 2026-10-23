package com.mortgage.reconciliation.service;

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
 * в порядке следования в документе: параграфы и ячейки таблиц.
 */
@Service
public class DocxReaderService {

    /**
     * Читает документ из файловой системы.
     *
     * @param path путь к .docx файлу
     * @return блоки документа в исходном порядке
     * @throws IOException если файл недоступен или не является корректным .docx
     */
    public List<DocxElement> readDocx(Path path) throws IOException {
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
    public List<DocxElement> readDocx(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream must not be null");

        List<DocxElement> elements = new ArrayList<>();
        int tableIndex = 0;

        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            for (IBodyElement bodyElement : document.getBodyElements()) {
                BodyElementType elementType = bodyElement.getElementType();

                if (elementType == BodyElementType.PARAGRAPH) {
                    XWPFParagraph paragraph = (XWPFParagraph) bodyElement;
                    elements.add(new DocxElement.ParagraphBlock(paragraph.getText()));
                } else if (elementType == BodyElementType.TABLE) {
                    XWPFTable table = (XWPFTable) bodyElement;
                    appendTableCells(table, tableIndex, elements);
                    tableIndex++;
                }
            }
        }

        return elements;
    }

    private void appendTableCells(XWPFTable table, int tableIndex, List<DocxElement> elements) {
        List<XWPFTableRow> rows = table.getRows();

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<XWPFTableCell> cells = rows.get(rowIndex).getTableCells();

            for (int colIndex = 0; colIndex < cells.size(); colIndex++) {
                elements.add(new DocxElement.TableCellBlock(
                        tableIndex,
                        rowIndex,
                        colIndex,
                        cells.get(colIndex).getText()
                ));
            }
        }
    }
}
