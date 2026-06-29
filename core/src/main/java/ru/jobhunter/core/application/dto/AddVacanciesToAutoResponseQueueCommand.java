package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.model.VacancySource;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record AddVacanciesToAutoResponseQueueCommand(
        UserId userId,
        List<AddVacancyToAutoResponseQueueCommand> vacancies
) {

    public AddVacanciesToAutoResponseQueueCommand {
        Objects.requireNonNull(userId, "User id must not be null");

        vacancies = List.copyOf(
                Objects.requireNonNull(
                        vacancies,
                        "Vacancies must not be null"
                )
        );

        if (vacancies.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one vacancy must be selected"
            );
        }

        Set<QueueVacancyKey> uniqueVacancies = new HashSet<>();

        for (AddVacancyToAutoResponseQueueCommand vacancy : vacancies) {
            Objects.requireNonNull(
                    vacancy,
                    "Queue vacancy command must not be null"
            );

            if (!userId.equals(vacancy.userId())) {
                throw new IllegalArgumentException(
                        "All queue vacancy commands must belong to one user"
                );
            }

            QueueVacancyKey key = new QueueVacancyKey(
                    vacancy.source(),
                    vacancy.externalVacancyId()
            );

            if (!uniqueVacancies.add(key)) {
                throw new IllegalArgumentException(
                        "Duplicate vacancy in batch: "
                                + vacancy.externalVacancyId()
                );
            }
        }
    }

    private record QueueVacancyKey(
            VacancySource source,
            String externalVacancyId
    ) {
    }
}