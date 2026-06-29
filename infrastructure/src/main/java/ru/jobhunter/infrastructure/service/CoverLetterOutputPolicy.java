package ru.jobhunter.infrastructure.service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class CoverLetterOutputPolicy {

    private static final Pattern CODE_FENCE_AT_START = Pattern.compile(
            "(?is)^```(?:text|markdown)?\\s*"
    );

    private static final Pattern CODE_FENCE_AT_END = Pattern.compile(
            "(?is)\\s*```$"
    );

    private static final Pattern THINKING_BLOCK = Pattern.compile(
            "(?is)<(?:think|analysis)>.*?</(?:think|analysis)>"
    );

    private static final Pattern THINKING_TAG = Pattern.compile(
            "(?is)</?(?:think|analysis)\\b[^>]*>"
    );

    private static final Pattern REASONING_PREFIX = Pattern.compile(
            "(?is)^\\s*(?:"
                    + "хорошо|итак|сначала|прежде всего|"
                    + "мне нужно|я должен|я должна|"
                    + "нужно|необходимо|следует|"
                    + "посмотрю|проверю|проанализирую|"
                    + "рассмотрим|давайте|"
                    + "the task is|i need to|let me"
                    + ")\\b"
    );

    private static final List<String> REASONING_MARKERS = List.of(
            "вакансия требует",
            "у кандидата",
            "резюме кандидата",
            "посмотрю на резюме",
            "нужно связать",
            "следует выбрать",
            "требования работодателя",
            "сначала я должен",
            "мне нужно подготовить",
            "нужно подготовить сопроводительное письмо"
    );

    private CoverLetterOutputPolicy() {
    }

    public static ValidationResult validate(
            String rawContent,
            int maxLength
    ) {
        String content = clean(rawContent);

        if (content.isBlank()) {
            return ValidationResult.rejected(
                    content,
                    "LLM returned an empty cover letter"
            );
        }

        if (content.length() > maxLength) {
            return ValidationResult.rejected(
                    content,
                    "cover letter exceeds "
                            + maxLength
                            + " characters"
            );
        }

        if (THINKING_TAG.matcher(content).find()) {
            return ValidationResult.rejected(
                    content,
                    "cover letter contains an unfinished reasoning block"
            );
        }

        if (looksLikePlainReasoning(content)) {
            return ValidationResult.rejected(
                    content,
                    "LLM returned planning or internal reasoning "
                            + "instead of a ready cover letter"
            );
        }

        return ValidationResult.accepted(content);
    }

    private static String clean(String rawContent) {
        String content = rawContent == null ? "" : rawContent.strip();

        content = CODE_FENCE_AT_START.matcher(content).replaceFirst("");
        content = CODE_FENCE_AT_END.matcher(content).replaceFirst("");
        content = THINKING_BLOCK.matcher(content).replaceAll("");

        return content.strip();
    }

    private static boolean looksLikePlainReasoning(String content) {
        String normalized = normalize(content);

        if (REASONING_PREFIX.matcher(normalized).find()) {
            return true;
        }

        long markerCount = REASONING_MARKERS.stream()
                .filter(normalized::contains)
                .count();

        return markerCount >= 2;
    }

    private static String normalize(String value) {
        return value
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    public record ValidationResult(
            String content,
            String rejectionReason
    ) {

        public static ValidationResult accepted(String content) {
            return new ValidationResult(content, "");
        }

        public static ValidationResult rejected(
                String content,
                String rejectionReason
        ) {
            return new ValidationResult(
                    content,
                    rejectionReason
            );
        }

        public boolean isAccepted() {
            return rejectionReason.isBlank();
        }
    }
}