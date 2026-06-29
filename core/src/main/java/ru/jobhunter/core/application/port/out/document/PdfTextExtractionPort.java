package ru.jobhunter.core.application.port.out.document;

public interface PdfTextExtractionPort {

    String extractText(byte[] pdfBytes);
}