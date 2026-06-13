package ru.jobhunter.core.application.usecase.integration;

import ru.jobhunter.core.application.dto.HhConnectionFlowDto;
import ru.jobhunter.core.domain.model.UserId;

public interface ConnectHhAccountUseCase {

    HhConnectionFlowDto startConnection(UserId userId);
}
