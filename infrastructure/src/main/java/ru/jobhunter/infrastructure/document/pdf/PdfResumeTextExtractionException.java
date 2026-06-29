package ru.jobhunter.infrastructure.document.pdf;

public class PdfResumeTextExtractionException extends RuntimeException {

    public PdfResumeTextExtractionException(String message) {
        super(message);
    }

    public PdfResumeTextExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}