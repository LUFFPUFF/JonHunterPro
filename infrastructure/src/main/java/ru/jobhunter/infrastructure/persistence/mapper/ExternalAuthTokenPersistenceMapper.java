package ru.jobhunter.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.jobhunter.core.domain.model.AuthProvider;
import ru.jobhunter.core.domain.model.ExternalAuthToken;
import ru.jobhunter.core.domain.model.ExternalAuthTokenId;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.infrastructure.persistence.entity.ExternalAuthTokenEntity;

@Mapper(componentModel = "spring")
public interface ExternalAuthTokenPersistenceMapper {

    @Mapping(target = "id", expression = "java(token.id().value())")
    @Mapping(target = "userId", expression = "java(token.userId().value())")
    @Mapping(target = "provider", expression = "java(token.provider().code())")
    @Mapping(target = "accessToken", expression = "java(token.accessToken())")
    @Mapping(target = "refreshToken", expression = "java(token.refreshToken())")
    @Mapping(target = "tokenType", expression = "java(token.tokenType())")
    @Mapping(target = "scope", expression = "java(token.scope())")
    @Mapping(target = "expiresAt", expression = "java(token.expiresAt())")
    @Mapping(target = "createdAt", expression = "java(token.createdAt())")
    @Mapping(target = "updatedAt", expression = "java(token.updatedAt())")
    ExternalAuthTokenEntity toEntity(ExternalAuthToken token);

    default ExternalAuthToken toDomain(ExternalAuthTokenEntity entity) {
        return ExternalAuthToken.restore(
                ExternalAuthTokenId.of(entity.getId()),
                UserId.of(entity.getUserId()),
                AuthProvider.fromCode(entity.getProvider()),
                entity.getAccessToken(),
                entity.getRefreshToken(),
                entity.getTokenType(),
                entity.getScope(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
