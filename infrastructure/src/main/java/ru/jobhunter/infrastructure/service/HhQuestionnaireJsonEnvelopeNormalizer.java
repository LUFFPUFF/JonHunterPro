package ru.jobhunter.infrastructure.service;

final class HhQuestionnaireJsonEnvelopeNormalizer {

    private HhQuestionnaireJsonEnvelopeNormalizer() {
    }

    static String normalize(String content) {
        String normalized = requireNotBlank(
                content,
                "LLM questionnaire form response must not be blank"
        );

        normalized = normalized.replaceFirst(
                "(?is)^```(?:json)?\\s*",
                ""
        );

        normalized = normalized.replaceFirst(
                "(?is)\\s*```$",
                ""
        ).trim();

        int objectStart = normalized.indexOf('{');
        int arrayStart = normalized.indexOf('[');

        int start = firstJsonStart(objectStart, arrayStart);

        if (start < 0) {
            throw new IllegalStateException(
                    "LLM questionnaire form does not contain JSON"
            );
        }

        char rootToken = normalized.charAt(start);

        if (rootToken == '[') {
            int end = normalized.lastIndexOf(']');

            if (end < start) {
                throw new IllegalStateException(
                        "LLM questionnaire form contains an incomplete JSON array"
                );
            }

            String answersArray = normalized.substring(
                    start,
                    end + 1
            );

            return "{\"answers\":" + answersArray + "}";
        }

        int end = normalized.lastIndexOf('}');

        if (end < start) {
            throw new IllegalStateException(
                    "LLM questionnaire form contains an incomplete JSON object"
            );
        }

        return normalized.substring(start, end + 1);
    }

    private static int firstJsonStart(
            int objectStart,
            int arrayStart
    ) {
        if (objectStart < 0) {
            return arrayStart;
        }

        if (arrayStart < 0) {
            return objectStart;
        }

        return Math.min(objectStart, arrayStart);
    }

    private static String requireNotBlank(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }

        return value.trim();
    }
}