package ru.rudn.rudnadmin.rest.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.rudn.rudnadmin.config.TestContainersBase;
import ru.rudn.rudnadmin.entity.*;
import ru.rudn.rudnadmin.entity.vpn.Vpn;
import ru.rudn.rudnadmin.rest.student.model.StudentUpdateDto;


import static ru.rudn.rudnadmin.testutil.TestDataUtils.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ru.rudn.rudnadmin.rest.global.utils.ExceptionHelperUtils.ERROR_DESCRIPTION_PREFIX;
import org.springframework.security.test.context.support.WithMockUser;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "test", roles = {"ADMIN"})
class StudentControllerTest extends TestContainersBase {

    private static final String PATH = "/api/students";
    private static final String PATH_WITH_ID = "/api/students/{id}";

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;


    @Test
    @DisplayName("Студенты: сценарий создать/получить/обновить/удалить")
    void crudFlow() throws Exception {
        final Direction dir = directionRepository.save(directionEntity1());
        final Group group = groupRepository.save(groupEntity1(dir));
        final User user = userRepository.save(userEntity1());

        // создаём
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentDto(group.getId(), user.getId()))))
                .andExpect(status().isCreated());

        final Student saved = studentRepository.findAll().get(0);

        // получаем (без VPN)
        mockMvc.perform(get(PATH_WITH_ID, saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(group.getId()))
                .andExpect(jsonPath("$.studentInfoDto[0].userId").value(user.getId()))
                .andExpect(jsonPath("$.studentInfoDto[0].email").value(user.getEmail()))
                .andExpect(jsonPath("$.studentInfoDto[0].vpn").value(org.hamcrest.Matchers.nullValue()));

        // обновляем
        final User user2 = userRepository.save(userEntity2());
        mockMvc.perform(put(PATH_WITH_ID, saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(StudentUpdateDto.builder().userId(user2.getId()).groupId(group.getId()).build())))
                .andExpect(status().isOk());

        final Student updated = studentRepository.findById(saved.getId()).orElseThrow();
        assertEquals(user2.getId(), updated.getUser().getId());

        // создаём VPN и проверяем, что он отражается в GET
        vpnRepository.save(Vpn.builder().user(updated.getUser()).link("link1").build());
        final Long vpnId = vpnRepository.findAll().get(0).getId();
        mockMvc.perform(get(PATH_WITH_ID, saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentInfoDto[0].vpn.id").value(vpnId));

        // удаляем VPN перед удалением студента, чтобы не нарушать FK
        vpnRepository.findByUser_Student_Id(saved.getId()).ifPresent(vpnRepository::delete);
        // удаляем
        mockMvc.perform(delete(PATH_WITH_ID, saved.getId()))
                .andExpect(status().isNoContent());
        assertEquals(0, studentRepository.count());
    }

    @Test
    @DisplayName("Студенты: id в запросе игнорируется")
    void createIdIgnored() throws Exception {
        final Direction dir = directionRepository.save(directionEntity1());
        final Group group = groupRepository.save(groupEntity1(dir));
        final User user = userRepository.save(userEntity1());

        final var dto = studentDto(group.getId(), user.getId());
        dto.getStudentInfoDto().get(0).setId(54321L);

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        final Student saved = studentRepository.findAll().get(0);
        assertNotEquals(54321L, saved.getId());
    }

    @Test
    @DisplayName("Студенты: 400 когда groupId/userId не существует")
    void createWrongRefs() throws Exception {
        // несуществующая группа
        final User user = userRepository.save(userEntity1());
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentDto(Long.MAX_VALUE, user.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(ERROR_DESCRIPTION_PREFIX + "Group"));

        // несуществующий пользователь
        final Direction dir = directionRepository.save(directionEntity1());
        final Group group = groupRepository.save(groupEntity1(dir));
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentDto(group.getId(), Long.MAX_VALUE))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(ERROR_DESCRIPTION_PREFIX + "User"));
    }

    @Test
    @DisplayName("Студенты: 400 когда studentInfoDto пустой")
    void createEmptyInfoList() throws Exception {
        final Direction dir = directionRepository.save(directionEntity1());
        final Group group = groupRepository.save(groupEntity1(dir));

        final var dto = studentDto(group.getId(), userEntity1().getId());
        dto.setStudentInfoDto(java.util.Collections.emptyList());

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("Поиск: студенты по названию группы")
    void studentsByGroupName() throws Exception {
        final Direction dir = directionRepository.save(directionEntity1());
        final Group group = groupRepository.save(groupEntity1(dir));
        final User u1 = userRepository.save(userEntity1());
        final User u2 = userRepository.save(userEntity2());

        studentRepository.save(Student.builder().group(group).user(u1).build());
        studentRepository.save(Student.builder().group(group).user(u2).build());

        mockMvc.perform(get(PATH + "/studentsByGroupName").param("name", group.getName())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(group.getId()))
                .andExpect(jsonPath("$.studentInfoDto.length()").value(2))
                .andExpect(jsonPath("$.studentInfoDto[0].userId").value(u1.getId()))
                .andExpect(jsonPath("$.studentInfoDto[0].email").value(u1.getEmail()))
                .andExpect(jsonPath("$.studentInfoDto[1].userId").value(u2.getId()))
                .andExpect(jsonPath("$.studentInfoDto[1].email").value(u2.getEmail()));
    }

    @Test
    @DisplayName("Студенты: 400 при удалении студента с активным VPN")
    void deleteStudentWithVpnReturnsBadRequest() throws Exception {
        final Direction dir = directionRepository.save(directionEntity1());
        final Group group = groupRepository.save(groupEntity1(dir));
        final User user = userRepository.save(userEntity1());

        final Student student = studentRepository.save(Student.builder().group(group).user(user).build());
        vpnRepository.save(Vpn.builder().user(user).link("link1").build());

        mockMvc.perform(delete(PATH_WITH_ID, student.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("Cannot delete student, while he has vpn.")))
                .andExpect(jsonPath("$.message").value(containsString(String.valueOf(student.getId()))));
    }

    @Test
    @DisplayName("Студенты: 400 при удалении студентов группы, когда у одного есть VPN")
    void deleteStudentsByGroupWithOneHavingVpnReturnsBadRequest() throws Exception {
        final Direction dir = directionRepository.save(directionEntity1());
        final Group group = groupRepository.save(groupEntity1(dir));

        final User u1 = userRepository.save(userEntity1());
        final User u2 = userRepository.save(userEntity2());

        studentRepository.save(Student.builder().group(group).user(u1).build());
        final Student s2 = studentRepository.save(Student.builder().group(group).user(u2).build());

        vpnRepository.save(Vpn.builder().user(u2).link("link2").build());

        mockMvc.perform(delete(PATH + "/group/{groupId}", group.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("Cannot delete students, while at least one has vpn.")))
                .andExpect(jsonPath("$.message").value(containsString(String.valueOf(s2.getId()))));
    }

    @Test
    @DisplayName("Студенты: удаление по группе — все студенты удалены")
    void deleteStudentsByGroupRemovesAll() throws Exception {
        final Direction dir = directionRepository.save(directionEntity1());
        final Group group = groupRepository.save(groupEntity1(dir));

        final User u1 = userRepository.save(userEntity1());
        final User u2 = userRepository.save(userEntity2());

        studentRepository.save(Student.builder().group(group).user(u1).build());
        studentRepository.save(Student.builder().group(group).user(u2).build());

        mockMvc.perform(delete(PATH + "/group/{groupId}", group.getId()))
                .andExpect(status().isNoContent());

        assertEquals(0, studentRepository.count());
    }

    @Test
    @DisplayName("Студенты: удаление по группе — удаляются только из этой группы")
    void deleteStudentsByGroupKeepsOtherGroups() throws Exception {
        final Direction dir1 = directionRepository.save(directionEntity1());
        final Direction dir2 = directionRepository.save(directionEntity2());
        final Group g1 = groupRepository.save(groupEntity1(dir1));
        final Group g2 = groupRepository.save(Group.builder().name("ИКБО-99-24").year((short) 2024).direction(dir2).build());

        final User u1 = userRepository.save(userEntity1());
        final User u2 = userRepository.save(userEntity2());
        final User u3 = userRepository.save(userEntity3());

        studentRepository.save(Student.builder().group(g1).user(u1).build());
        studentRepository.save(Student.builder().group(g1).user(u2).build());
        final Student other = studentRepository.save(Student.builder().group(g2).user(u3).build());

        mockMvc.perform(delete(PATH + "/group/{groupId}", g1.getId()))
                .andExpect(status().isNoContent());

        // В группе g1 никого не осталось
        assertTrue(studentRepository.findStudentsByGroup_Id(g1.getId()).isEmpty());
        // Студент из другой группы остался
        assertEquals(1, studentRepository.count());
        assertTrue(studentRepository.findById(other.getId()).isPresent());
    }
}
