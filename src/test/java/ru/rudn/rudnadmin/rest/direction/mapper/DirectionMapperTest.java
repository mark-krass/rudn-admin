package ru.rudn.rudnadmin.rest.direction.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.rudn.rudnadmin.entity.Direction;
import ru.rudn.rudnadmin.rest.direction.model.DirectionDto;

import static org.junit.jupiter.api.Assertions.*;
import static ru.rudn.rudnadmin.testutil.TestDataUtils.directionDto2;

@SpringBootTest(classes = {DirectionMapperImpl.class})
class DirectionMapperTest {

    @Autowired
    private DirectionMapper mapper;

    @Test
    @DisplayName("DirectionMapper: toEntity маппит поля и игнорирует id")
    void toEntity() {
        final DirectionDto dto = directionDto2();

        final Direction entity = mapper.toEntity(dto);
        assertNull(entity.getId());
        assertEquals("Математика", entity.getName());
        assertEquals("01.03.01", entity.getCode());
        assertTrue(entity.getIsActive());
    }

    @Test
    @DisplayName("DirectionMapper: updateEntity игнорирует null-значения")
    void updateEntityIgnoreNulls() {
        final Direction entity = Direction.builder()
                .id(1L)
                .name("OLD")
                .code("OLD")
                .isActive(true)
                .build();

        final DirectionDto patch = DirectionDto.builder()
                .name(null)
                .code("NEW")
                .isActive(null)
                .build();

        mapper.updateEntity(patch, entity);

        assertEquals("OLD", entity.getName());
        assertEquals("NEW", entity.getCode());
        assertTrue(entity.getIsActive());
        assertEquals(1L, entity.getId());
    }
}
