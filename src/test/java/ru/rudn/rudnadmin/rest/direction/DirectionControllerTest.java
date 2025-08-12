package ru.rudn.rudnadmin.rest.direction;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.rudn.rudnadmin.config.TestContainersBase;
import ru.rudn.rudnadmin.entity.Direction;
import ru.rudn.rudnadmin.rest.direction.model.DirectionDto;
import static ru.rudn.rudnadmin.testutil.TestDataUtils.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ru.rudn.rudnadmin.rest.global.utils.ExceptionHelperUtils.ERROR_DESCRIPTION_PREFIX;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "test", roles = {"ADMIN"})
class DirectionControllerTest extends TestContainersBase {

    private static final String PATH = "/api/directions";
    private static final String PATH_WITH_ID = "/api/directions/{id}";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Направления: успешное создание двух направлений")
    void createDirectionsSuccess() throws Exception {
        final DirectionDto dto1 = directionDto1();
        final DirectionDto dto2 = directionDto2();
        final List<DirectionDto> directions = Arrays.asList(dto1, dto2);

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(directions)))
                .andExpect(status().isCreated());

        final List<Direction> savedDirections = directionRepository.findAll();
        assertEquals(2, savedDirections.size());

        final Direction saved1 = savedDirections.stream()
                .filter(d -> d.getName().equals("Информационные технологии"))
                .findFirst().get();
        assertEquals("09.03.01", saved1.getCode());
        assertTrue(saved1.getIsActive());

        final Direction saved2 = savedDirections.stream()
                .filter(d -> d.getName().equals("Математика"))
                .findFirst().get();
        assertEquals("01.03.01", saved2.getCode());
        assertTrue(saved2.getIsActive());
    }

    @Test
    @DisplayName("Направления: пустой список для создания")
    void createDirectionsEmptyList() throws Exception {
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.emptyList())))
                .andExpect(status().isCreated());

        assertEquals(0, directionRepository.count());
    }

    @Test
    @DisplayName("Направления: id в запросе игнорируется")
    void createDirections_IdIgnored() throws Exception {
        final DirectionDto dto1 = directionDto1();
        dto1.setId(777L);
        final DirectionDto dto2 = directionDto2();
        dto2.setId(888L);

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(dto1, dto2))))
                .andExpect(status().isCreated());

        final List<Direction> saved = directionRepository.findAll();
        assertEquals(2, saved.size());
        assertNotEquals(777L, saved.get(0).getId());
        assertNotEquals(888L, saved.get(1).getId());
    }

    @Test
    @DisplayName("Направления: ошибка валидации при некорректных данных")
    void createDirections_ValidationError() throws Exception {
        final DirectionDto invalidDto = invalidDirectionDto();

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(invalidDto))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        assertEquals(0, directionRepository.count());
    }

    @Test
    @DisplayName("Направления: успешное получение списка всех направлений")
    void getDirections_Success() throws Exception {
        final Direction direction1 = directionEntity1();
        final Direction direction2 = directionEntity2();

        directionRepository.saveAll(Arrays.asList(direction1, direction2));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Информационные технологии"))
                .andExpect(jsonPath("$[0].code").value("09.03.01"))
                .andExpect(jsonPath("$[0].is_active").value(true))
                .andExpect(jsonPath("$[1].name").value("Математика"))
                .andExpect(jsonPath("$[1].code").value("01.03.01"))
                .andExpect(jsonPath("$[1].is_active").value(true));
    }

    @Test
    @DisplayName("Направления: получение пустого списка направлений")
    void getDirections_EmptyList() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Направления: успешное получение направления по ID")
    void getDirection_Success() throws Exception {
        final Direction direction = directionEntity1();
        final Direction savedDirection = directionRepository.save(direction);

        mockMvc.perform(get(PATH_WITH_ID, savedDirection.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(savedDirection.getId()))
                .andExpect(jsonPath("$.name").value("Информационные технологии"))
                .andExpect(jsonPath("$.code").value("09.03.01"))
                .andExpect(jsonPath("$.is_active").value(true));
    }

    @Test
    @DisplayName("Направления: направление не найдено по несуществующему ID")
    void getDirection_NotFound() throws Exception {
        mockMvc.perform(get(PATH + "/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(ERROR_DESCRIPTION_PREFIX + "Direction"));
    }

    @Test
    @DisplayName("Направления: успешное обновление существующего направления")
    void updateDirection_Success() throws Exception {
        final Direction savedDirection = directionRepository.save(directionEntity1());

        final DirectionDto updateDto = directionDto2();

        mockMvc.perform(put(PATH_WITH_ID, savedDirection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());

        final Direction updatedDirection = directionRepository.findById(savedDirection.getId()).orElse(null);
        assertEquals("Математика", updatedDirection.getName());
        assertEquals("01.03.01", updatedDirection.getCode());
        assertTrue(updatedDirection.getIsActive());
    }

    @Test
    @DisplayName("Направления: направление не найдено для обновления")
    void updateDirection_NotFound() throws Exception {
        final DirectionDto updateDto = directionDto2();

        mockMvc.perform(put(PATH + "/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(ERROR_DESCRIPTION_PREFIX + "Direction"));
    }

    @Test
    @DisplayName("Направления: ошибка валидации при обновлении некорректными данными")
    void updateDirection_ValidationError() throws Exception {
        final Direction savedDirection = directionRepository.save(directionEntity1());

        final DirectionDto invalidDto = invalidDirectionDto();

        mockMvc.perform(put(PATH_WITH_ID, savedDirection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        // Проверяем, что данные не изменились
        final Direction unchangedDirection = directionRepository.findById(savedDirection.getId()).orElse(null);
        assertEquals("Информационные технологии", unchangedDirection.getName());
        assertEquals("09.03.01", unchangedDirection.getCode());
        assertTrue(unchangedDirection.getIsActive());
    }

    @Test
    @DisplayName("Направления: успешное удаление существующего направления")
    void deleteDirection_Success() throws Exception {
        final Direction existingDirection = directionEntity1();
        final Direction savedDirection = directionRepository.save(existingDirection);

        mockMvc.perform(delete(PATH_WITH_ID, savedDirection.getId()))
                .andExpect(status().isNoContent());

        assertFalse(directionRepository.existsById(savedDirection.getId()));
        assertEquals(0, directionRepository.count());
    }

    @Test
    @DisplayName("Направления: направление не найдено для удаления")
    void deleteDirection_NotFound() throws Exception {
        mockMvc.perform(delete(PATH + "/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Направления: удаление одного из нескольких с проверкой оставшегося")
    void deleteDirection_WithMultipleDirections() throws Exception {
        final Direction direction1 = directionEntity1();
        final Direction direction2 = directionEntity2();

        final Direction savedDirection1 = directionRepository.save(direction1);
        final Direction savedDirection2 = directionRepository.save(direction2);

        mockMvc.perform(delete(PATH_WITH_ID, savedDirection1.getId()))
                .andExpect(status().isNoContent());

        assertFalse(directionRepository.existsById(savedDirection1.getId()));
        assertTrue(directionRepository.existsById(savedDirection2.getId()));
        assertEquals(1, directionRepository.count());

        final Direction remainingDirection = directionRepository.findById(savedDirection2.getId()).orElse(null);
        assertEquals("Математика", remainingDirection.getName());
        assertEquals("01.03.01", remainingDirection.getCode());
    }
}