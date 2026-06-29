package ru.jobhunter.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import ru.jobhunter.core.domain.model.Resume;
import ru.jobhunter.core.domain.model.ResumeId;
import ru.jobhunter.core.domain.model.ResumeSourceType;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.infrastructure.persistence.entity.ResumeEntity;

import java.util.Objects;

@Component
public final class ResumePersistenceMapper {

    public ResumeEntity toEntity(Resume resume) {
        Objects.requireNonNull(resume, "Resume must not be null");

        ResumeEntity entity = new ResumeEntity();

        entity.setId(resume.id().value());
        entity.setUserId(resume.userId().value());
        entity.setTitle(resume.title());
        entity.setSourceType(resume.sourceType().code());
        entity.setOriginalFileName(resume.originalFileName());
        entity.setContent(resume.content());
        entity.setPrimaryResume(resume.primary());
        entity.setCreatedAt(resume.createdAt());
        entity.setUpdatedAt(resume.updatedAt());

        return entity;
    }

    public Resume toDomain(ResumeEntity entity) {
        Objects.requireNonNull(entity, "Resume entity must not be null");

        return new Resume(
                ResumeId.of(entity.getId()),
                UserId.of(entity.getUserId()),
                entity.getTitle(),
                ResumeSourceType.fromCode(entity.getSourceType()),
                entity.getOriginalFileName(),
                entity.getContent(),
                entity.isPrimaryResume(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}