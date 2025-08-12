package ru.rudn.rudnadmin.rest.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.rudn.rudnadmin.config.TestContainersBase;
import ru.rudn.rudnadmin.entity.User;
import ru.rudn.rudnadmin.rest.user.model.UserDto;
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
class UserControllerTest extends TestContainersBase {

    private static final String PATH = "/api/users";
    private static final String PATH_WITH_ID = "/api/users/{id}";

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;


    @Test
    @DisplayName("Пользователи: сценарий создать/получить/обновить/удалить")
    void crud_flow() throws Exception {
        // создаём
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(userDto1()))))
                .andExpect(status().isCreated());

        final User saved = userRepository.findAll().get(0);

        // получаем
        mockMvc.perform(get(PATH_WITH_ID, saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));

        // обновляем
        final UserDto upd = userDto2();
        mockMvc.perform(put(PATH_WITH_ID, saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upd)))
                .andExpect(status().isOk());

        final User updated = userRepository.findById(saved.getId()).orElseThrow();
        assertEquals("user2@example.com", updated.getEmail());

        // удаляем
        mockMvc.perform(delete(PATH_WITH_ID, saved.getId()))
                .andExpect(status().isNoContent());
        assertEquals(0, userRepository.count());
    }

    @Test
    @DisplayName("Пользователи: получение несуществующего — 404 и описание")
    void get_notFound() throws Exception {
        mockMvc.perform(get(PATH_WITH_ID, 99999L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(ERROR_DESCRIPTION_PREFIX + "User"));
    }

    @Test
    @DisplayName("Пользователи: обновление несуществующего — 404 и описание")
    void update_notFound() throws Exception {
        mockMvc.perform(put(PATH_WITH_ID, 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto1())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(ERROR_DESCRIPTION_PREFIX + "User"));
    }

    @Test
    @DisplayName("Пользователи: обновление — ошибка валидации 400 и сообщение")
    void update_validationError() throws Exception {
        final User saved = userRepository.save(userEntity1());

        mockMvc.perform(put(PATH_WITH_ID, saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUserDto())))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        // Данные не должны измениться
        final User unchanged = userRepository.findById(saved.getId()).orElseThrow();
        assertEquals(userEntity1().getEmail(), unchanged.getEmail());
    }

    @Test
    @DisplayName("Пользователи: удаление несуществующего — 404 и описание")
    void delete_notFound() throws Exception {
        mockMvc.perform(delete(PATH_WITH_ID, 99999L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(ERROR_DESCRIPTION_PREFIX + "User"));
    }

    @Test
    @DisplayName("Пользователи: id в запросе игнорируется")
    void create_id_ignored() throws Exception {
        final var dto = userDto1();
        dto.setId(12345L);

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(dto))))
                .andExpect(status().isCreated());

        final User saved = userRepository.findAll().get(0);
        assertNotEquals(12345L, saved.getId());
    }

    @Test
    @DisplayName("Пользователи: валидация 400 на некорректном запросе")
    void create_validation() throws Exception {
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(invalidUserDto()))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        // дублирующий email → ожидаем 400/409 (сейчас может быть 500, проверим, что не 2xx)
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(userDto1()))))
                .andExpect(status().isCreated());

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(userDto1()))))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }
}
