package ru.jobhunter.core.domain.exception;

public final class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String email) {
        super("User already exists for email: " + email);
    }
}
