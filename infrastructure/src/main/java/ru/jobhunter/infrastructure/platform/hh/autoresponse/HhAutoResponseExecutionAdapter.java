package ru.jobhunter.infrastructure.platform.hh.autoresponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.dto.AutoResponseExecutionRequest;
import ru.jobhunter.core.application.dto.AutoResponseExecutionResultDto;
import ru.jobhunter.core.application.port.out.AutoResponseExecutionPort;
import ru.jobhunter.core.domain.model.VacancySource;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
public final class HhAutoResponseExecutionAdapter implements AutoResponseExecutionPort {

    private static final Logger log = LoggerFactory.getLogger(HhAutoResponseExecutionAdapter.class);

    @Override
    public boolean supports(VacancySource source) {
        return VacancySource.HH_RU == source;
    }

    @Override
    public CompletableFuture<AutoResponseExecutionResultDto> execute(
            AutoResponseExecutionRequest request
    ) {
        Objects.requireNonNull(request, "Auto response execution request must not be null");

        if (!supports(request.source())) {
            return CompletableFuture.completedFuture(
                    AutoResponseExecutionResultDto.notAvailable(
                            request.queueItemId(),
                            request.source(),
                            request.externalVacancyId(),
                            "HH.ru auto response adapter does not support source: " + request.source()
                    )
            );
        }

        log.info(
                "HH.ru auto response execution requested but not available yet: userId={}, queueItemId={}, externalVacancyId={}",
                request.userId(),
                request.queueItemId(),
                request.externalVacancyId()
        );

        return CompletableFuture.completedFuture(
                AutoResponseExecutionResultDto.notAvailable(
                        request.queueItemId(),
                        request.source(),
                        request.externalVacancyId(),
                        "HH.ru auto response execution is not available yet. HH.ru OAuth/API access is not approved or configured."
                )
        );
    }
}