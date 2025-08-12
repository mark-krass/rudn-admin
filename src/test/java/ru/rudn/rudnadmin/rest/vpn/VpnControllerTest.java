package ru.rudn.rudnadmin.rest.vpn;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.rudn.rudnadmin.config.TestContainersBase;
import ru.rudn.rudnadmin.entity.Direction;
import ru.rudn.rudnadmin.entity.Group;
import ru.rudn.rudnadmin.entity.Student;
import ru.rudn.rudnadmin.entity.User;
import ru.rudn.rudnadmin.entity.vpn.TaskType;
import ru.rudn.rudnadmin.service.vpn.scheduler.VpnTaskScheduler;
import ru.rudn.rudnadmin.service.openvpn.VpnService;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ru.rudn.rudnadmin.testutil.TestDataUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.security.test.context.support.WithMockUser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "test", roles = {"ADMIN"})
class VpnControllerTest extends TestContainersBase {

    private static final String PATH_GROUP = "/api/vpn/group/{groupId}";
    private static final String PATH_STUDENT = "/api/vpn/student/{studentId}";
    private static final String PATH_CREATE_FOR_GROUP = "/api/vpn/createForGroup/{groupId}";
    private static final String PATH_CREATE_FOR_STUDENT = "/api/vpn/createForStudent/{studentId}";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private VpnTaskScheduler scheduler;
    @MockitoBean
    private VpnService vpnService;

    @Test
    @DisplayName("VPN: создать для группы, получить по группе/студенту и удалить")
    void vpnFlow() throws Exception {
        final Direction dir = directionRepository.save(directionEntity1());
        final Group group = groupRepository.save(groupEntity1(dir));
        final User u1 = userRepository.save(userEntity1());
        final User u2 = userRepository.save(userEntity2());
        final Student s1 = studentRepository.save(Student.builder().group(group).user(u1).build());
        final Student s2 = studentRepository.save(Student.builder().group(group).user(u2).build()); // второй студент для проверки списка
        assertNotNull(s2.getId());

        // создаём для группы
        mockMvc.perform(post(PATH_CREATE_FOR_GROUP, group.getId()))
                .andExpect(status().isAccepted());

        // задачи на создание созданы для обоих студентов
        assertEquals(2, vpnTaskRepository.findVpnTasksByIsActiveTrueAndTypeEquals(TaskType.CREATE_OVPN).size());

        // получаем по группе
        mockMvc.perform(get(PATH_GROUP, group.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // получаем по студенту
        mockMvc.perform(get(PATH_STUDENT, s1.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(u1.getId()));

        // запускаем шедулер, чтобы закрыть задачи на создание
        // мок: OpenVpnService ничего не возвращает
        doNothing().when(vpnService).createRemoteVpn(anyLong());
        // подготовим файлы, которые должен был создать скрипт
        final Path outDir = Paths.get("build", "vpn-out");
        Files.createDirectories(outDir);
        final Path sample = Paths.get("src", "test", "resources", "some-connect.ovpn");
        final Long vpnId1 = vpnRepository.findByUser_Student_Id(s1.getId()).orElseThrow().getId();
        final Long vpnId2 = vpnRepository.findByUser_Student_Id(s2.getId()).orElseThrow().getId();
        Files.copy(sample, outDir.resolve(vpnId1 + ".ovpn"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(sample, outDir.resolve(vpnId2 + ".ovpn"), StandardCopyOption.REPLACE_EXISTING);
        scheduler.createVpnForAllTasks();

        mockMvc.perform(delete(PATH_STUDENT, s1.getId()))
                .andExpect(status().isAccepted());
        // проверяем, что пока нет задачи DELETE_OVPN (сначала идёт DELETE_MINIO)
        assertEquals(0, vpnTaskRepository.findVpnTasksByIsActiveTrueAndTypeEquals(TaskType.DELETE_OVPN).size());

        // на этом этапе должна появиться активная задача DELETE_MINIO (первый шаг delete flow)
        assertEquals(1, vpnTaskRepository.findVpnTasksByIsActiveTrueAndTypeEquals(TaskType.DELETE_MINIO).size());

        // запускаем шедулер: произойдет удаление VPN и каскад удалит задачи
        scheduler.createVpnForAllTasks();
        // у s2 есть закрытая задача на создание, добавляется ещё одна на удаление — итого 2 задачи
        mockMvc.perform(delete(PATH_GROUP, group.getId()))
                .andExpect(status().isAccepted());

        assertEquals(1, vpnTaskRepository.findVpnTasksByIsActiveTrueAndTypeEquals(TaskType.DELETE_MINIO).size());
        assertEquals(2, vpnTaskRepository.findAll().size());

        // запускается удаление, каскадно удаляется и закрытая задача на создание
        scheduler.createVpnForAllTasks();
        for (TaskType type : TaskType.values()) {
            assertEquals(0, vpnTaskRepository.findVpnTasksByIsActiveTrueAndTypeEquals(type).size());
        }
        assertEquals(0, vpnTaskRepository.findAll().size());
    }

    @Test
    @DisplayName("VPN: негативный — конфликт при создании дублирующей активной задачи для того же VPN")
    void negativeConflictOnDuplicateActiveTask() throws Exception {
        final Direction dir = directionRepository.save(directionEntity1());
        final Group group = groupRepository.save(groupEntity1(dir));
        final User u1 = userRepository.save(userEntity1());
        final Student s1 = studentRepository.save(Student.builder().group(group).user(u1).build());

        // первый вызов createForGroup создаёт активные задачи CREATE
        mockMvc.perform(post(PATH_CREATE_FOR_GROUP, group.getId()))
                .andExpect(status().isAccepted());

        // пытаемся добавить ещё одну задачу CREATE для того же VPN повторным createForGroup
        mockMvc.perform(post(PATH_CREATE_FOR_GROUP, group.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Data integrity violation"));

        // пытаемся добавить задачу DELETE при существующей активной CREATE
        mockMvc.perform(delete(PATH_STUDENT, s1.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Data integrity violation"));
    }

    @Test
    @DisplayName("VPN: негативный — создание второго VPN для того же студента приводит к конфликту")
    void negativeDuplicateVpnForSameStudent() throws Exception {
        final Direction dir = directionRepository.save(directionEntity1());
        final Group group = groupRepository.save(groupEntity1(dir));
        final User u1 = userRepository.save(userEntity1());
        final Student s1 = studentRepository.save(Student.builder().group(group).user(u1).build());

        // первый createForStudent
        mockMvc.perform(post(PATH_CREATE_FOR_STUDENT, s1.getId()))
                .andExpect(status().isAccepted());

        assertEquals(1, vpnRepository.findAll().size());
        assertEquals(1, vpnTaskRepository.findAll().size());

        // второй createForStudent для того же студента нарушает уникальность (один VPN на студента)
        mockMvc.perform(post(PATH_CREATE_FOR_STUDENT, s1.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Data integrity violation"));

        // проверяем в репозиториях: по-прежнему один VPN и нет дубликатов задач
        assertEquals(1, vpnRepository.findAll().size());
        assertEquals(1, vpnTaskRepository.findAll().size());
    }

    @Test
    @DisplayName("VPN: обновление ссылки по студенту — создаётся задача RENEW_LINK")
    void renewByStudentCreatesTask() throws Exception {
        final Direction dir = directionRepository.save(directionEntity1());
        final Group group = groupRepository.save(groupEntity1(dir));
        final User u1 = userRepository.save(userEntity1());
        final Student s1 = studentRepository.save(Student.builder().group(group).user(u1).build());

        // создаём VPN для студента
        mockMvc.perform(post(PATH_CREATE_FOR_STUDENT, s1.getId()))
                .andExpect(status().isAccepted());
        doNothing().when(vpnService).createRemoteVpn(anyLong());
        // подготовим файл для этого студента
        final Path outDir2 = Paths.get("build", "vpn-out");
        Files.createDirectories(outDir2);
        final Path sample2 = Paths.get("src", "test", "resources", "some-connect.ovpn");
        final Long vpnIdRenew = vpnRepository.findByUser_Student_Id(s1.getId()).orElseThrow().getId();
        Files.copy(sample2, outDir2.resolve(vpnIdRenew + ".ovpn"), StandardCopyOption.REPLACE_EXISTING);
        scheduler.createVpnForAllTasks();

        assertEquals(0, vpnTaskRepository.findVpnTasksByIsActiveTrueAndTypeEquals(TaskType.RENEW_LINK).size());

        // инициируем renew по студенту
        mockMvc.perform(put(PATH_STUDENT, s1.getId()))
                .andExpect(status().isAccepted());

        assertEquals(1, vpnTaskRepository.findVpnTasksByIsActiveTrueAndTypeEquals(TaskType.RENEW_LINK).size());
    }
}


