package ru.rudn.rudnadmin.rest.vpn.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import ru.rudn.rudnadmin.entity.vpn.Vpn;
import ru.rudn.rudnadmin.rest.vpn.model.VpnDto;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface VpnMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user.id", source = "userId")
    Vpn toEntity(VpnDto dto);

    @Mapping(target = "userId", source = "user.id")
    VpnDto toResponse(Vpn entity);

    List<VpnDto> toResponse(List<Vpn> entities);
}


