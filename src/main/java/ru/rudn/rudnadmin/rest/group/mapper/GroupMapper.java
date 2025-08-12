package ru.rudn.rudnadmin.rest.group.mapper;

import org.mapstruct.*;
import ru.rudn.rudnadmin.entity.Group;
import ru.rudn.rudnadmin.rest.group.model.GroupDto;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface GroupMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "direction", ignore = true)
    Group toEntity(GroupDto dto);

    @Mapping(target = "directionId", source = "direction.id")
    GroupDto toResponse(Group entity);

    List<GroupDto> toResponse(List<Group> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "direction", ignore = true)
    void updateEntity(GroupDto dto, @MappingTarget Group entity);
}
