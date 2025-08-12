package ru.rudn.rudnadmin.rest.student.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.rudn.rudnadmin.entity.Group;
import ru.rudn.rudnadmin.entity.Student;
import ru.rudn.rudnadmin.entity.User;
import ru.rudn.rudnadmin.entity.vpn.Vpn;
import ru.rudn.rudnadmin.rest.student.model.StudentInfoDto;
import ru.rudn.rudnadmin.rest.vpn.mapper.VpnMapperImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {StudentMapperImpl.class, VpnMapperImpl.class})
class StudentMapperTest {

    @Autowired
    private StudentMapper mapper;

    @Test
    @DisplayName("StudentMapper: toEntity из StudentInfoDto устанавливает user и игнорирует id/group")
    void toEntity() {
        final StudentInfoDto info = StudentInfoDto.builder().userId(2L).build();
        final Student entity = mapper.toEntity(info);
        assertNull(entity.getId());
        assertNull(entity.getGroup());
        assertNotNull(entity.getUser());
        assertEquals(2L, entity.getUser().getId());
    }

    @Test
    @DisplayName("StudentMapper: toResponseDto маппит groupId и список studentInfoDto с id и userId")
    void toResponseDto() {
        final Group group = Group.builder().id(10L).name("G").year((short) 2024).build();
        final User user = User.builder().id(20L).email("e@e").build();
        final Student entity = Student.builder().id(5L).group(group).user(user).build();

        final var dto = mapper.toResponse(entity);
        assertEquals(10L, dto.getGroupId());
        assertEquals(1, dto.getStudentInfoDto().size());
        assertEquals(5L, dto.getStudentInfoDto().get(0).getId());
        assertEquals(20L, dto.getStudentInfoDto().get(0).getUserId());
        assertEquals("e@e", dto.getStudentInfoDto().get(0).getEmail());
    }

    @Test
    @DisplayName("StudentMapper: маппит vpn объект в StudentInfoDto")
    void mapsVpn() {
        final Group group = Group.builder().id(1L).name("G").year((short) 2024).build();
        final User user = User.builder().id(2L).email("u@e").build();
        final Student student = Student.builder().id(5L).group(group).user(user).build();
        final Vpn vpn = Vpn.builder().id(7L).user(user).link("link1").build();
        user.setVpn(vpn);
        user.setStudent(student);

        final var dto = mapper.toResponse(student);

        assertNotNull(dto.getStudentInfoDto());
        assertEquals(1, dto.getStudentInfoDto().size());
        final var info = dto.getStudentInfoDto().get(0);
        assertNotNull(info.getVpn());
        assertEquals(7L, info.getVpn().getId());
        assertEquals(2L, info.getVpn().getUserId());
        assertEquals("link1", info.getVpn().getLink());
    }

    @Test
    @DisplayName("SearchMapper: пустой вход возвращает dto с null groupId и пустым списком")
    void emptyInput() {
        final var dto = mapper.toStudents(List.of());
        assertNull(dto.getGroupId());
        assertNotNull(dto.getStudentInfoDto());
        assertTrue(dto.getStudentInfoDto().isEmpty());
    }

    @Test
    @DisplayName("SearchMapper: маппит groupId и строит список student info")
    void mapsList() {
        final Group group = Group.builder().id(1L).name("G").year((short) 2024).build();
        final User u1 = User.builder().id(10L).email("a@a").build();
        final User u2 = User.builder().id(20L).email("b@b").build();
        final Student s1 = Student.builder().id(100L).group(group).user(u1).build();
        final Student s2 = Student.builder().id(200L).group(group).user(u2).build();

        final var dto = mapper.toStudents(List.of(s1, s2));
        assertEquals(1L, dto.getGroupId());
        assertEquals(2, dto.getStudentInfoDto().size());
        assertTrue(dto.getStudentInfoDto().stream().anyMatch(i -> i.getId().equals(100L) && i.getUserId().equals(10L)));
        assertTrue(dto.getStudentInfoDto().stream().anyMatch(i -> i.getId().equals(200L) && i.getUserId().equals(20L)));
        assertTrue(dto.getStudentInfoDto().stream().anyMatch(i -> "a@a".equals(i.getEmail())));
        assertTrue(dto.getStudentInfoDto().stream().anyMatch(i -> "b@b".equals(i.getEmail())));
    }
}
