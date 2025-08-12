package ru.rudn.rudnadmin.rest.user.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.rudn.rudnadmin.entity.User;
import ru.rudn.rudnadmin.rest.user.model.UserDto;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    @DisplayName("UserMapper: toEntity маппит поля и игнорирует id")
    void toEntity() {
        final UserDto dto = UserDto.builder()
                .email("user@example.com")
                .name("John")
                .middleName("J.")
                .lastname("Doe")
                .build();
        final User entity = mapper.toEntity(dto);
        assertNull(entity.getId());
        assertEquals("user@example.com", entity.getEmail());
        assertEquals("John", entity.getName());
        assertEquals("J.", entity.getMiddleName());
        assertEquals("Doe", entity.getLastname());
    }

    @Test
    @DisplayName("UserMapper: updateEntity игнорирует null-значения")
    void updateEntity_ignoreNulls() {
        final User entity = User.builder().id(1L).email("old@e").name("Old").lastname("Old").build();
        final UserDto patch = UserDto.builder().email(null).name("New").build();

        mapper.updateEntity(patch, entity);
        assertEquals("old@e", entity.getEmail());
        assertEquals("New", entity.getName());
        assertEquals(1L, entity.getId());
    }
}
