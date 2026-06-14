package ru.jobhunter.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import ru.jobhunter.core.domain.model.AutoResponseQueueItem;
import ru.jobhunter.core.domain.model.AutoResponseQueueItemId;
import ru.jobhunter.core.domain.model.AutoResponseQueueStatus;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.model.VacancySource;
import ru.jobhunter.infrastructure.persistence.entity.AutoResponseQueueItemEntity;

@Component
public class AutoResponseQueueItemPersistenceMapper {

    public AutoResponseQueueItemEntity toEntity(AutoResponseQueueItem item) {
        AutoResponseQueueItemEntity entity = new AutoResponseQueueItemEntity();

        entity.setId(item.id().value());
        entity.setUserId(item.userId().value());
        entity.setSource(item.source().code());
        entity.setExternalVacancyId(item.externalVacancyId());
        entity.setVacancyName(item.vacancyName());
        entity.setEmployerName(item.employerName());
        entity.setAreaName(item.areaName());
        entity.setVacancyUrl(item.vacancyUrl());
        entity.setStatus(item.status().name());
        entity.setCreatedAt(item.createdAt());
        entity.setUpdatedAt(item.updatedAt());

        return entity;
    }

    public AutoResponseQueueItem toDomain(AutoResponseQueueItemEntity entity) {
        return new AutoResponseQueueItem(
                AutoResponseQueueItemId.of(entity.getId()),
                UserId.of(entity.getUserId()),
                VacancySource.fromCode(entity.getSource()),
                entity.getExternalVacancyId(),
                entity.getVacancyName(),
                entity.getEmployerName(),
                entity.getAreaName(),
                entity.getVacancyUrl(),
                AutoResponseQueueStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
