package ru.jobhunter.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.jobhunter.core.domain.model.Email;
import ru.jobhunter.core.domain.model.FullName;
import ru.jobhunter.core.domain.model.PasswordHash;
import ru.jobhunter.core.domain.model.User;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.infrastructure.persistence.entity.UserEntity;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {

    @Mapping(target = "id", expression = "java(user.id().value())")
    @Mapping(target = "email", expression = "java(user.email().value())")
    @Mapping(target = "passwordHash", expression = "java(user.passwordHash().value())")
    @Mapping(target = "fullName", expression = "java(user.fullName().value())")
    @Mapping(target = "createdAt", expression = "java(user.createdAt())")
    @Mapping(target = "updatedAt", expression = "java(user.updatedAt())")
    UserEntity toEntity(User user);

    @Mapping(target = "id", source = "id", qualifiedByName = "uuidToUserId")
    @Mapping(target = "email", source = "email", qualifiedByName = "stringToEmail")
    @Mapping(target = "passwordHash", source = "passwordHash", qualifiedByName = "stringToPasswordHash")
    @Mapping(target = "fullName", source = "fullName", qualifiedByName = "stringToFullName")
    User toDomain(UserEntity entity);

    @Named("uuidToUserId")
    default UserId uuidToUserId(UUID value) {
        return UserId.of(value);
    }

    @Named("stringToEmail")
    default Email stringToEmail(String value) {
        return Email.of(value);
    }

    @Named("stringToPasswordHash")
    default PasswordHash stringToPasswordHash(String value) {
        return PasswordHash.of(value);
    }

    @Named("stringToFullName")
    default FullName stringToFullName(String value) {
        return FullName.of(value);
    }
}