package com.mortgage.reconciliation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortgage.reconciliation.dto.BlockType;
import com.mortgage.reconciliation.dto.DocBlock;
import com.mortgage.reconciliation.service.DocxReaderService;
import com.mortgage.reconciliation.service.TextNormalizer;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DebugControllerTest {

    private final DocxReaderService docxReaderService = new DocxReaderService(new TextNormalizer());
    private final DebugController controller = new DebugController(docxReaderService);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsBlocksForUploadedDocx() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contract.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                buildDocx());

        List<DocBlock> blocks = controller.readDocx(file);

        assertThat(blocks).hasSize(3);
        assertThat(blocks.get(0).type()).isEqualTo(BlockType.PARAGRAPH);
        assertThat(blocks.get(0).text()).isEqualTo("Договор");
        assertThat(blocks.get(0).tableIndex()).isNull();

        assertThat(blocks.get(1).type()).isEqualTo(BlockType.TABLE_CELL);
        assertThat(blocks.get(1).tableIndex()).isZero();
        assertThat(blocks.get(1).rowIndex()).isZero();
        assertThat(blocks.get(1).colIndex()).isZero();
        assertThat(blocks.get(1).text()).isEqualTo("Сумма");
        assertThat(blocks.get(2).colIndex()).isEqualTo(1);
    }

    @Test
    void serializesBlocksWithTypeAndWithoutNullCoordinates() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "contract.docx", null, buildDocx());

        String json = objectMapper.writeValueAsString(controller.readDocx(file));

        assertThat(json).contains("\"type\":\"PARAGRAPH\"");
        assertThat(json).contains("\"type\":\"TABLE_CELL\"");
        assertThat(json).contains("\"tableIndex\":0");
        assertThat(json).doesNotContain("tableIndex\":null");
    }

    @Test
    void rejectsNonDocxFile() {
        MockMultipartFile file = new MockMultipartFile("file", "contract.pdf", "application/pdf", "no".getBytes());

        assertThatThrownBy(() -> controller.readDocx(file))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.docx", null, new byte[0]);

        assertThatThrownBy(() -> controller.readDocx(file))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private byte[] buildDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            document.createParagraph().createRun().setText("Договор");

            XWPFTable table = document.createTable(1, 2);
            table.getRow(0).getCell(0).setText("Сумма");
            table.getRow(0).getCell(1).setText("1 000 000");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }
}
