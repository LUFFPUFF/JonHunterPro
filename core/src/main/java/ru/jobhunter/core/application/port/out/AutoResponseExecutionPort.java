package ru.jobhunter.core.application.port.out;

import ru.jobhunter.core.application.dto.AutoResponseExecutionRequest;
import ru.jobhunter.core.application.dto.AutoResponseExecutionResultDto;
import ru.jobhunter.core.domain.model.VacancySource;

import java.util.concurrent.CompletableFuture;

public interface AutoResponseExecutionPort {

    boolean supports(VacancySource source);

    CompletableFuture<AutoResponseExecutionResultDto> execute(AutoResponseExecutionRequest request);
}
