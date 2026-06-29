package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.UserId;

import java.util.Locale;
import java.util.Objects;

public record UploadPrimaryResumePdfCommand(
        UserId userId,
        String originalFileName,
        byte[] pdfBytes
) {

    public static final int MAX_PDF_SIZE_BYTES = 15 * 1024 * 1024;

    public UploadPrimaryResumePdfCommand {
        Objects.requireNonNull(userId, "User id must not be null");

        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("Resume PDF file name must not be blank");
        }

        originalFileName = originalFileName.trim();

        if (!originalFileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("Resume file must have PDF extension");
        }

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("Resume PDF file must not be empty");
        }

        if (pdfBytes.length > MAX_PDF_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "Resume PDF file must not exceed "
                            + MAX_PDF_SIZE_BYTES / (1024 * 1024)
                            + " MB"
            );
        }

        pdfBytes = pdfBytes.clone();
    }

    @Override
    public byte[] pdfBytes() {
        return pdfBytes.clone();
    }
}