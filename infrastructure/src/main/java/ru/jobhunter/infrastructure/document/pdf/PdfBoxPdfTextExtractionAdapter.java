package ru.jobhunter.infrastructure.document.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.port.out.document.PdfTextExtractionPort;

import java.io.IOException;
import java.util.Objects;

@Component
public final class PdfBoxPdfTextExtractionAdapter implements PdfTextExtractionPort {

    private static final int MAX_EXTRACTED_TEXT_LENGTH = 120_000;

    @Override
    public String extractText(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new PdfResumeTextExtractionException(
                    "Resume PDF file must not be empty"
            );
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (document.getNumberOfPages() == 0) {
                throw new PdfResumeTextExtractionException(
                        "Resume PDF does not contain pages"
                );
            }

            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);

            String extractedText = normalize(textStripper.getText(document));

            if (extractedText.isBlank()) {
                throw new PdfResumeTextExtractionException(
                        "Could not extract text from resume PDF. "
                                + "The file may be a scanned document without a text layer."
                );
            }

            if (extractedText.length() > MAX_EXTRACTED_TEXT_LENGTH) {
                throw new PdfResumeTextExtractionException(
                        "Extracted resume text is too large"
                );
            }

            return extractedText;
        } catch (PdfResumeTextExtractionException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new PdfResumeTextExtractionException(
                    "Could not read resume PDF",
                    exception
            );
        }
    }

    private String normalize(String text) {
        Objects.requireNonNull(text, "Extracted PDF text must not be null");

        return text
                .replace('\u0000', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll("[ ]{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }
}