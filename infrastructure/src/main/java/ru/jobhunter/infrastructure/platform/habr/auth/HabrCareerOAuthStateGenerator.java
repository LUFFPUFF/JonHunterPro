package ru.jobhunter.infrastructure.platform.habr.auth;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

@Component
public class HabrCareerOAuthStateGenerator {

    private final SecureRandom secureRandom = new SecureRandom();
    private final HabrCareerOAuthProperties properties;

    public HabrCareerOAuthStateGenerator(HabrCareerOAuthProperties properties) {
        this.properties = Objects.requireNonNull(
                properties,
                "Habr Career OAuth properties must not be null"
        );
    }

    public String generate() {
        int byteLength = properties.stateByteLength();

        if (byteLength < 16) {
            throw new HabrCareerOAuthConfigurationException(
                    "Habr Career OAuth state byte length must be at least 16"
            );
        }

        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
