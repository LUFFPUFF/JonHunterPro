package ru.jobhunter.infrastructure.platform.hh.autoresponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.jobhunter.infrastructure.platform.hh.api.HhApiClient;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhSuitableResumeItemResponse;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhSuitableResumesResponse;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
public class HhSuitableResumeResolver {

    private static final Logger log = LoggerFactory.getLogger(HhSuitableResumeResolver.class);

    private final HhApiClient apiClient;
    private final HhAutoResponseProperties properties;

    public HhSuitableResumeResolver(
            HhApiClient apiClient,
            HhAutoResponseProperties properties
    ) {
        this.apiClient = Objects.requireNonNull(apiClient, "HH API client must not be null");
        this.properties = Objects.requireNonNull(properties, "HH auto response properties must not be null");
    }

    public CompletableFuture<String> resolveResumeId(
            String vacancyId,
            String accessToken
    ) {
        if (properties.hasDefaultResumeId()) {
            return CompletableFuture.completedFuture(properties.defaultResumeId());
        }

        return apiClient.getSuitableResumes(vacancyId, accessToken)
                .thenApply(response -> selectResumeId(vacancyId, response));
    }

    private String selectResumeId(
            String vacancyId,
            HhSuitableResumesResponse response
    ) {
        if (response.items() == null || response.items().isEmpty()) {
            throw new HhAutoResponseExecutionException(
                    "HH.ru did not return suitable resumes for vacancy: " + vacancyId
            );
        }

        HhSuitableResumeItemResponse selectedResume = response.items().stream()
                .filter(resume -> resume.id() != null && !resume.id().isBlank())
                .min(Comparator.comparing(resume -> normalize(resume.title())))
                .orElseThrow(() -> new HhAutoResponseExecutionException(
                        "HH.ru suitable resumes response does not contain resume id for vacancy: " + vacancyId
                ));

        log.info(
                "HH.ru suitable resume selected: vacancyId={}, resumeTitleProvided={}",
                vacancyId,
                selectedResume.title() != null && !selectedResume.title().isBlank()
        );

        return selectedResume.id().trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}