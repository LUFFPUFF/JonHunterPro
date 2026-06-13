package ru.jobhunter.infrastructure.platform.hh.auth;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class HhOAuthStateGenerator {

    private final SecureRandom secureRandom = new SecureRandom();
    private final HhOAuthProperties properties;

    public HhOAuthStateGenerator(HhOAuthProperties properties) {
        this.properties = properties;
    }

    public String generate() {
        int byteLength = properties.stateByteLength();

        if (byteLength < 16) {
            throw new HhOAuthConfigurationException("HH OAuth state byte length must be at least 16");
        }

        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
