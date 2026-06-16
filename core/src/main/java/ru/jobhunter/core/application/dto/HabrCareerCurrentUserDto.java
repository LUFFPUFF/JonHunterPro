package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.AuthProvider;

import java.time.Instant;
import java.util.Objects;

public record HabrCareerCurrentUserDto(
        AuthProvider provider,
        String login,
        String email,
        String firstName,
        String lastName,
        String middleName,
        String birthday,
        String avatar,
        String city,
        String country,
        String gender,
        Instant loadedAt
) {

    public HabrCareerCurrentUserDto {
        Objects.requireNonNull(provider, "Auth provider must not be null");
        Objects.requireNonNull(loadedAt, "Loaded timestamp must not be null");

        login = normalize(login);
        email = normalize(email);
        firstName = normalize(firstName);
        lastName = normalize(lastName);
        middleName = normalize(middleName);
        birthday = normalize(birthday);
        avatar = normalize(avatar);
        city = normalize(city);
        country = normalize(country);
        gender = normalize(gender);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}