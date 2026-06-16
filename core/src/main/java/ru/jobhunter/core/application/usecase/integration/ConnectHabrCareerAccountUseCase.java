package ru.jobhunter.core.application.usecase.integration;

import ru.jobhunter.core.application.dto.HabrCareerConnectionFlowDto;
import ru.jobhunter.core.domain.model.UserId;

public interface ConnectHabrCareerAccountUseCase {

    HabrCareerConnectionFlowDto startConnection(UserId userId);
}