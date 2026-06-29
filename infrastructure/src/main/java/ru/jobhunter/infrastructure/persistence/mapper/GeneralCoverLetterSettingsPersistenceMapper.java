package ru.jobhunter.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import ru.jobhunter.core.domain.model.GeneralCoverLetterSettings;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.infrastructure.persistence.entity.GeneralCoverLetterSettingsEntity;

import java.util.Objects;

@Component
public class GeneralCoverLetterSettingsPersistenceMapper {

    public GeneralCoverLetterSettingsEntity toEntity(
            GeneralCoverLetterSettings settings
    ) {
        Objects.requireNonNull(
                settings,
                "General cover letter settings must not be null"
        );

        GeneralCoverLetterSettingsEntity entity =
                new GeneralCoverLetterSettingsEntity();

        entity.setUserId(settings.userId().value());
        entity.setContent(settings.content());
        entity.setUseWhenLlmUnavailable(
                settings.useWhenLlmUnavailable()
        );
        entity.setSourceFileName(settings.sourceFileName());
        entity.setCreatedAt(settings.createdAt());
        entity.setUpdatedAt(settings.updatedAt());

        return entity;
    }

    public GeneralCoverLetterSettings toDomain(
            GeneralCoverLetterSettingsEntity entity
    ) {
        Objects.requireNonNull(
                entity,
                "General cover letter settings entity must not be null"
        );

        return GeneralCoverLetterSettings.restore(
                UserId.of(entity.getUserId()),
                entity.getContent(),
                entity.isUseWhenLlmUnavailable(),
                entity.getSourceFileName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}