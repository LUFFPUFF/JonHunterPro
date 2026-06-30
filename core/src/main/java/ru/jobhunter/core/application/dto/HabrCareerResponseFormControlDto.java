package ru.jobhunter.core.application.dto;

public record HabrCareerResponseFormControlDto(
        String tagName,
        String inputType,
        String name,
        String id,
        String placeholder,
        String label,
        boolean required,
        boolean disabled
) {

    public HabrCareerResponseFormControlDto {
        tagName = normalize(tagName);
        inputType = normalize(inputType);
        name = normalize(name);
        id = normalize(id);
        placeholder = normalize(placeholder);
        label = normalize(label);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
