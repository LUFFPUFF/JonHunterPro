package ru.jobhunter.infrastructure.platform.hh.autoresponse;

import java.util.Objects;

/**
 * Verifies that the text stored in a browser textarea matches the prepared
 * cover letter before the response form can be submitted.
 */
final class HhCoverLetterDomValueVerifier {

    private HhCoverLetterDomValueVerifier() {
    }

    static HhCoverLetterDomValueVerification verify(
            String expectedValue,
            String actualValue
    ) {
        String normalizedExpected = normalizeExpected(expectedValue);
        String normalizedActual = normalizeActual(actualValue);

        return new HhCoverLetterDomValueVerification(
                normalizedExpected.equals(normalizedActual),
                normalizedExpected.length(),
                normalizedActual.length()
        );
    }

    private static String normalizeExpected(String value) {
        Objects.requireNonNull(
                value,
                "Expected cover letter must not be null"
        );

        String normalized = normalizeLineEndings(value).strip();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "Expected cover letter must not be blank"
            );
        }

        return normalized;
    }

    private static String normalizeActual(String value) {
        if (value == null) {
            return "";
        }

        return normalizeLineEndings(value).strip();
    }

    private static String normalizeLineEndings(String value) {
        return value
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }
}