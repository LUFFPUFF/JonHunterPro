package ru.jobhunter.core.domain.model;

public enum ResumeSourceType {

    UPLOADED_PDF,
    UPLOADED_DOCX,
    HH_RU,
    MANUAL;

    public String code() {
        return name();
    }

    public static ResumeSourceType fromCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Resume source type must not be blank");
        }

        try {
            return ResumeSourceType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unsupported resume source type: " + value,
                    exception
            );
        }
    }
}