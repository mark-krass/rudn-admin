package ru.rudn.rudnadmin.rest.vpn.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.rudn.rudnadmin.entity.Student;
import ru.rudn.rudnadmin.entity.User;
import ru.rudn.rudnadmin.entity.vpn.Vpn;
import ru.rudn.rudnadmin.rest.vpn.model.VpnDto;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {VpnMapperImpl.class})
class VpnMapperTest {

    @Autowired
    private VpnMapper mapper;

    @Test
    @DisplayName("VpnMapper: toDto маппит user.id в userId")
    void toDtoMapsStudentId() {
        final User user = User.builder().id(9L).build();
        final Student student = Student.builder().id(7L).user(user).build();
        user.setStudent(student);
        final Vpn entity = Vpn.builder().id(3L).user(user).link("url").build();
        final VpnDto dto = mapper.toResponse(entity);
        assertEquals(3L, dto.getId());
        assertEquals(9L, dto.getUserId());
        assertEquals("url", dto.getLink());
    }
}


