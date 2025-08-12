package ru.rudn.rudnadmin.rest.direction.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import ru.rudn.rudnadmin.entity.Direction;
import ru.rudn.rudnadmin.rest.direction.model.DirectionDto;

import java.util.List;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface DirectionMapper {

    /**
     * Маппинг из DirectionRequestDto в Direction Entity
     */
    @Mapping(target = "id", ignore = true)
    Direction toEntity(DirectionDto dto);

    List<Direction> toEntity(List<DirectionDto> dto);

    /**
     * Маппинг из Direction Entity в DirectionResponseDto
     */
    DirectionDto toResponse(Direction entity);

    /**
     * Маппинг списка Entity в список ResponseDto
     */
    List<DirectionDto> toResponse(List<Direction> entities);

    /**
     * Обновление существующего Entity данными из RequestDto
     */
    @Mapping(target = "id", ignore = true)
    void updateEntity(DirectionDto dto, @MappingTarget Direction entity);
}
