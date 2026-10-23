package com.mortgage.reconciliation.service;

import com.mortgage.reconciliation.dto.BlockType;
import com.mortgage.reconciliation.dto.DocBlock;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocxReaderServiceTest {

    private final DocxReaderService service = new DocxReaderService(new TextNormalizer());

    @Test
    void readsParagraphsAndTableCellsInDocumentOrder() throws Exception {
        byte[] docx = buildDocx();

        List<DocBlock> blocks;
        try (InputStream inputStream = new ByteArrayInputStream(docx)) {
            blocks = service.readDocx(inputStream);
        }

        assertThat(blocks).hasSize(6);

        assertThat(blocks.get(0).type()).isEqualTo(BlockType.PARAGRAPH);
        assertThat(blocks.get(0).text()).isEqualTo("Договор ипотеки");
        assertThat(blocks.get(0).tableIndex()).isNull();

        assertThat(blocks.get(1).type()).isEqualTo(BlockType.TABLE_CELL);
        assertThat(blocks.get(1).tableIndex()).isZero();
        assertThat(blocks.get(1).rowIndex()).isZero();
        assertThat(blocks.get(1).colIndex()).isZero();
        assertThat(blocks.get(1).text()).isEqualTo("Сумма");

        assertThat(blocks.get(2).rowIndex()).isZero();
        assertThat(blocks.get(2).colIndex()).isEqualTo(1);
        assertThat(blocks.get(2).text()).isEqualTo("1 000 000");

        assertThat(blocks.get(3).rowIndex()).isEqualTo(1);
        assertThat(blocks.get(3).colIndex()).isZero();
        assertThat(blocks.get(3).text()).isEqualTo("Срок");

        assertThat(blocks.get(5).type()).isEqualTo(BlockType.PARAGRAPH);
        assertThat(blocks.get(5).text()).isEqualTo("Подпись");
    }

    @Test
    void normalizesExtractedText() throws Exception {
        assertThat(new TextNormalizer().normalize("a\u00A0b   c"))
                .isEqualTo("a b c");
        assertThat(new TextNormalizer().normalize("\u00ABСумма\u00BB \u2014 10\u00A0%"))
                .isEqualTo("\"Сумма\" - 10 %");
        assertThat(new TextNormalizer().normalize("  много\u2007пробелов  "))
                .isEqualTo("много пробелов");
        assertThat(new TextNormalizer().normalize(null)).isEmpty();
    }

    private byte[] buildDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph title = document.createParagraph();
            title.createRun().setText("Договор ипотеки");

            XWPFTable table = document.createTable(2, 2);
            table.getRow(0).getCell(0).setText("Сумма");
            table.getRow(0).getCell(1).setText("1 000 000");
            table.getRow(1).getCell(0).setText("Срок");
            table.getRow(1).getCell(1).setText("20 лет");

            XWPFParagraph footer = document.createParagraph();
            footer.createRun().setText("Подпись");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }
}
