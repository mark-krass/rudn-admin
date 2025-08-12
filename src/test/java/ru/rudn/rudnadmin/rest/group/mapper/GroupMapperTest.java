package ru.rudn.rudnadmin.rest.group.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.rudn.rudnadmin.entity.Direction;
import ru.rudn.rudnadmin.entity.Group;
import ru.rudn.rudnadmin.rest.group.model.GroupDto;

import static org.junit.jupiter.api.Assertions.*;
import static ru.rudn.rudnadmin.testutil.TestDataUtils.*;

@SpringBootTest(classes = {GroupMapperImpl.class})
class GroupMapperTest {

    @Autowired
    private GroupMapper mapper;

    @Test
    @DisplayName("GroupMapper: toEntity игнорирует id и direction")
    void toEntity() {
        final GroupDto dto = groupDto1(1L);

        final Group entity = mapper.toEntity(dto);
        assertNull(entity.getId());
        assertEquals("ИКБО-01-24", entity.getName());
        assertEquals(Short.valueOf((short) 2024), entity.getYear());
        assertNull(entity.getDirection());
    }

    @Test
    @DisplayName("GroupMapper: toResponseDto маппит directionId")
    void toResponseDto() {
        final Direction dir = directionEntity1();
        dir.setId(10L);
        final Group entity = groupEntity1(dir);
        entity.setId(5L);

        final var dto = mapper.toResponse(entity);
        assertEquals(5L, dto.getId());
        assertEquals("ИКБО-01-24", dto.getName());
        assertEquals(Short.valueOf((short) 2024), dto.getYear());
        assertEquals(10L, dto.getDirectionId());
    }

    @Test
    @DisplayName("GroupMapper: updateEntity игнорирует null-значения и direction")
    void updateEntityIgnoreNulls() {
        final Group entity = groupEntity1(new Direction());
        entity.setId(1L);
        entity.setName("OLD");
        entity.setYear((short) 2020);
        final GroupDto patch = GroupDto.builder().name(null).year((short) 2025).directionId(99L).build();

        mapper.updateEntity(patch, entity);
        assertEquals("OLD", entity.getName());
        assertEquals(Short.valueOf((short) 2025), entity.getYear());
        assertNotNull(entity.getDirection());
        assertEquals(1L, entity.getId());
    }
}
