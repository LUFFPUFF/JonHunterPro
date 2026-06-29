package ru.jobhunter.infrastructure.document.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfBoxPdfTextExtractionAdapterTest {

    private final PdfBoxPdfTextExtractionAdapter adapter =
            new PdfBoxPdfTextExtractionAdapter();

    @Test
    void shouldExtractTextFromPdf() throws Exception {
        byte[] pdfBytes = createPdf("Java Spring Boot PostgreSQL");

        String extractedText = adapter.extractText(pdfBytes);

        assertTrue(extractedText.contains("Java Spring Boot PostgreSQL"));
    }

    @Test
    void shouldRejectPdfWithoutTextLayer() throws Exception {
        byte[] pdfBytes = createEmptyPdf();

        PdfResumeTextExtractionException exception = assertThrows(
                PdfResumeTextExtractionException.class,
                () -> adapter.extractText(pdfBytes)
        );

        assertTrue(exception.getMessage().contains("Could not extract text"));
    }

    private byte[] createPdf(String text) throws IOException {
        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream =
                         new PDPageContentStream(document, page)) {

                contentStream.beginText();
                contentStream.setFont(
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                        12
                );
                contentStream.newLineAtOffset(72, 720);
                contentStream.showText(text);
                contentStream.endText();
            }

            document.save(outputStream);

            return outputStream.toByteArray();
        }
    }

    private byte[] createEmptyPdf() throws IOException {
        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            document.addPage(new PDPage());
            document.save(outputStream);

            return outputStream.toByteArray();
        }
    }
}