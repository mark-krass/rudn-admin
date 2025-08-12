package ru.rudn.rudnadmin.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import ru.rudn.rudnadmin.config.TestContainersBase;
import ru.rudn.rudnadmin.entity.Direction;
import ru.rudn.rudnadmin.entity.Group;
import ru.rudn.rudnadmin.entity.Student;
import ru.rudn.rudnadmin.entity.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ru.rudn.rudnadmin.testutil.TestDataUtils.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserRepositoryTest extends TestContainersBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @DisplayName("UserRepository: находит всех пользователей без записи Student")
    void findAllByStudentIsNullBasic() {
        final User u1 = userRepository.save(userEntity1());
        final User u2 = userRepository.save(userEntity2());
        final User u3 = userRepository.save(userEntity3());

        final Direction dir = directionRepository.save(directionEntity1());
        final Group group = groupRepository.save(groupEntity1(dir));

        studentRepository.save(Student.builder().group(group).user(u1).build());

        final List<User> result = userRepository.findAllByStudentIsNull();
        assertEquals(2, result.size());
        final var ids = result.stream().map(User::getId).toList();
        assertTrue(ids.contains(u2.getId()));
        assertTrue(ids.contains(u3.getId()));
        assertFalse(ids.contains(u1.getId()));
    }

    @Test
    @DisplayName("UserRepository: возвращает пусто, когда у всех есть Student")
    void findAllByStudentIsNullEmptyWhenAllHaveStudent() {
        final User u1 = userRepository.save(userEntity1());
        final User u2 = userRepository.save(userEntity2());

        final Direction dir = directionRepository.save(directionEntity1());
        final Group group = groupRepository.save(groupEntity1(dir));

        studentRepository.save(Student.builder().group(group).user(u1).build());
        studentRepository.save(Student.builder().group(group).user(u2).build());

        final List<User> result = userRepository.findAllByStudentIsNull();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}


