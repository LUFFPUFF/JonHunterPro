package ru.jobhunter.ui.session;

import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.dto.AuthenticatedUserDto;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class CurrentUserSession {

    private final AtomicReference<AuthenticatedUserDto> currentUser = new AtomicReference<>();

    public void setCurrentUser(AuthenticatedUserDto user) {
        currentUser.set(user);
    }

    public Optional<AuthenticatedUserDto> getCurrentUser() {
        return Optional.ofNullable(currentUser.get());
    }

    public boolean isAuthenticated() {
        return currentUser.get() != null;
    }

    public void clear() {
        currentUser.set(null);
    }
}
