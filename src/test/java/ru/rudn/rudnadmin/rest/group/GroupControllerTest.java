package ru.rudn.rudnadmin.rest.group;

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
import ru.rudn.rudnadmin.entity.Group;
import ru.rudn.rudnadmin.rest.group.model.GroupDto;
import static ru.rudn.rudnadmin.testutil.TestDataUtils.*;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ru.rudn.rudnadmin.rest.global.utils.ExceptionHelperUtils.ERROR_DESCRIPTION_PREFIX;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "test", roles = {"ADMIN"})
class GroupControllerTest extends TestContainersBase {

    private static final String PATH = "/api/groups";
    private static final String PATH_WITH_ID = "/api/groups/{id}";

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;


    @Test
    @DisplayName("Группы: успешное создание")
    void createSuccess() throws Exception {
        final Direction dir = directionRepository.save(directionEntity1());
        final GroupDto dto = groupDto1(dir.getId());

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(dto))))
                .andExpect(status().isCreated());

        assertEquals(1, groupRepository.count());
    }

    @Test
    @DisplayName("Группы: валидация 400 при некорректном запросе")
    void createValidation() throws Exception {
        final Direction dir = directionRepository.save(directionEntity1());
        final GroupDto dto = invalidGroupDto(dir.getId());

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(dto))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        // directionId не существует → ожидаем 400
        final GroupDto wrongDirection = groupDto1(Long.MAX_VALUE);
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(wrongDirection))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(ERROR_DESCRIPTION_PREFIX + "Direction"));
    }

    @Test
    @DisplayName("Группы: сценарий создать/получить/обновить/удалить")
    void crudFlow() throws Exception {
        final Direction dir = directionRepository.save(directionEntity1());
        // создаём
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(groupDto1(dir.getId())))))
                .andExpect(status().isCreated());

        final Group group = groupRepository.findAll().get(0);

        // получаем
        mockMvc.perform(get(PATH_WITH_ID, group.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ИКБО-01-24"))
                .andExpect(jsonPath("$.directionId").value(dir.getId()));

        // обновляем
        final Direction dir2 = directionRepository.save(directionEntity2());
        final GroupDto upd = groupDto2(dir2.getId());
        mockMvc.perform(put(PATH_WITH_ID, group.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upd)))
                .andExpect(status().isOk());

        final Group updated = groupRepository.findById(group.getId()).orElseThrow();
        assertEquals("ИКБО-02-24", updated.getName());
        assertEquals(dir2.getId(), updated.getDirection().getId());

        // удаляем
        mockMvc.perform(delete(PATH_WITH_ID, group.getId()))
                .andExpect(status().isNoContent());
        assertEquals(0, groupRepository.count());
    }

    @Test
    @DisplayName("Группы: id в запросе игнорируется")
    void createIdIgnored() throws Exception {
        final Direction dir = directionRepository.save(directionEntity1());
        final GroupDto dto = groupDto1(dir.getId());
        dto.setId(999L);

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(dto))))
                .andExpect(status().isCreated());

        final Group saved = groupRepository.findAll().get(0);
        assertNotEquals(999L, saved.getId());
    }

    @Test
    @DisplayName("Группы: не найдена группа по ID — 404 с описанием")
    void get_NotFound() throws Exception {
        mockMvc.perform(get(PATH_WITH_ID, 9999L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Cannot find such Group"));
    }

    @Test
    @DisplayName("Группы: обновление — неверный directionId приводит к 400 и описанию")
    void update_WrongDirection_BadRequest() throws Exception {
        final Direction dir = directionRepository.save(directionEntity1());
        final Group group = groupRepository.save(groupEntity1(dir));

        final GroupDto upd = groupDto1(Long.MAX_VALUE);
        mockMvc.perform(put(PATH_WITH_ID, group.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upd)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Cannot find such Direction"));
    }
}
