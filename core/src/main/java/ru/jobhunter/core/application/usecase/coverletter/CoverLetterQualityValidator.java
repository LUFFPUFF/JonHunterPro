package ru.jobhunter.core.application.usecase.coverletter;

import java.util.ArrayList;
import java.util.List;

public final class CoverLetterQualityValidator {

    public static final int MIN_LENGTH = 250;
    public static final int MAX_LENGTH = 4_000;
    public static final int MIN_SENTENCE_COUNT = 2;

    private CoverLetterQualityValidator() {
    }

    public static String validateAndNormalize(String coverLetter) {
        if (coverLetter == null || coverLetter.isBlank()) {
            throw new GeneratedCoverLetterQualityException(
                    "LLM returned an empty cover letter"
            );
        }

        String normalized = normalize(coverLetter);
        int length = normalized.length();
        int sentenceCount = countSentences(normalized);

        List<String> violations = getViolations(length, sentenceCount);

        if (!violations.isEmpty()) {
            throw new GeneratedCoverLetterQualityException(
                    "Generated cover letter did not pass quality gate: "
                            + String.join("; ", violations)
            );
        }

        return normalized;
    }

    private static List<String> getViolations(int length, int sentenceCount) {
        List<String> violations = new ArrayList<>();

        if (length < MIN_LENGTH) {
            violations.add(
                    "length=%d, minimum=%d".formatted(length, MIN_LENGTH)
            );
        }

        if (length > MAX_LENGTH) {
            violations.add(
                    "length=%d, maximum=%d".formatted(length, MAX_LENGTH)
            );
        }

        if (sentenceCount < MIN_SENTENCE_COUNT) {
            violations.add(
                    "sentences=%d, minimum=%d".formatted(
                            sentenceCount,
                            MIN_SENTENCE_COUNT
                    )
            );
        }
        return violations;
    }

    private static String normalize(String value) {
        return value
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll("[ ]{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    private static int countSentences(String text) {
        int count = 0;
        boolean containsMeaningfulCharacter = false;

        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);

            if (Character.isLetterOrDigit(character)) {
                containsMeaningfulCharacter = true;
                continue;
            }

            if (containsMeaningfulCharacter
                    && (character == '.' || character == '!' || character == '?')) {
                count++;
                containsMeaningfulCharacter = false;
            }
        }

        return count;
    }
}