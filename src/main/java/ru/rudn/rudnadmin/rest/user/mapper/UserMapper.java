package ru.rudn.rudnadmin.rest.user.mapper;

import org.mapstruct.*;
import ru.rudn.rudnadmin.entity.User;
import ru.rudn.rudnadmin.rest.user.model.UserDto;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    User toEntity(UserDto dto);

    List<User> toEntity(List<UserDto> dto);

    UserDto toResponse(User entity);

    List<UserDto> toResponse(List<User> entities);

    @Mapping(target = "id", ignore = true)
    void updateEntity(UserDto dto, @MappingTarget User entity);
}
