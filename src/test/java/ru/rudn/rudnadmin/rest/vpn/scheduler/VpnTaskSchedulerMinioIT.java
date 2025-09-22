package ru.rudn.rudnadmin.rest.vpn.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.rudn.rudnadmin.config.TestContainersBase;
import ru.rudn.rudnadmin.entity.Student;
import ru.rudn.rudnadmin.entity.vpn.TaskType;
import ru.rudn.rudnadmin.entity.vpn.Vpn;
import ru.rudn.rudnadmin.entity.vpn.VpnTask;
import ru.rudn.rudnadmin.service.openvpn.VpnService;
import ru.rudn.rudnadmin.service.vpn.scheduler.VpnTaskScheduler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static ru.rudn.rudnadmin.testutil.TestDataUtils.*;

@SpringBootTest
class VpnTaskSchedulerMinioIT extends TestContainersBase {

    @Autowired
    private VpnTaskScheduler scheduler;

    @MockitoBean
    private VpnService vpnService;

    @Value("${minio.bucket.name}")
    private String bucket;

    @Test
    @DisplayName("Шедулер с MinIO: успешное создание и удаление, и ошибки в задачах")
    void schedulerMinioSuccessAndErrors() throws Exception {
        // подготовка данных
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var u1 = userRepository.save(userEntity1());
        final var u2 = userRepository.save(userEntity2());
        studentRepository.save(Student.builder().group(group).user(u1).build());
        studentRepository.save(Student.builder().group(group).user(u2).build());

        final Vpn vpn1 = vpnRepository.save(Vpn.builder().user(u1).build());
        final Vpn vpn2 = vpnRepository.save(Vpn.builder().user(u2).build());
        final VpnTask t1 = vpnTaskRepository.save(VpnTask.builder().vpn(vpn1).type(TaskType.CREATE_OVPN).build());
        final VpnTask t2 = vpnTaskRepository.save(VpnTask.builder().vpn(vpn2).type(TaskType.CREATE_OVPN).build());

        // Подготавливаем выходные файлы, которые должен был бы создать OpenVPN-скрипт
        final Path outDir = Paths.get("build", "vpn-out");
        Files.createDirectories(outDir);
        final Path sample = Paths.get("src", "test", "resources", "some-connect.ovpn");
        Files.copy(sample, outDir.resolve(vpn1.getId() + ".ovpn"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(sample, outDir.resolve(vpn2.getId() + ".ovpn"), StandardCopyOption.REPLACE_EXISTING);

        // мок: создание ничего не возвращает
        doNothing().when(vpnService).createRemoteVpn(anyLong());

        // действие: создание
        scheduler.createVpnForAllTasks();

        // проверка: создано
        assertFalse(vpnTaskRepository.findById(t1.getId()).orElseThrow().getIsActive());
        assertFalse(vpnTaskRepository.findById(t2.getId()).orElseThrow().getIsActive());
        assertNotNull(vpnRepository.findById(vpn1.getId()).orElseThrow().getLink());
        assertNotNull(vpnRepository.findById(vpn2.getId()).orElseThrow().getLink());

        // добавляем задачи на удаление
        final VpnTask del1 = vpnTaskRepository.save(VpnTask.builder().vpn(vpn1).type(TaskType.DELETE_OVPN).build());
        final VpnTask del2 = vpnTaskRepository.save(VpnTask.builder().vpn(vpn2).type(TaskType.DELETE_OVPN).build());

        // действие: удаление (единый запуск шедулера)
        scheduler.createVpnForAllTasks();

        assertTrue(vpnRepository.findById(vpn1.getId()).isEmpty());
        assertTrue(vpnRepository.findById(vpn2.getId()).isEmpty());
        assertTrue(vpnTaskRepository.findById(del1.getId()).isEmpty());
        assertTrue(vpnTaskRepository.findById(del2.getId()).isEmpty());

        // негативные сценарии
        final Vpn vpn3 = vpnRepository.save(Vpn.builder().user(u1).build());
        final VpnTask t3 = vpnTaskRepository.save(VpnTask.builder().vpn(vpn3).type(TaskType.CREATE_OVPN).build());
        doThrow(new RuntimeException("ovpn-fail")).when(vpnService).createRemoteVpn(anyLong());
        scheduler.createVpnForAllTasks();
        assertTrue(vpnTaskRepository.findById(t3.getId()).orElseThrow().getIsActive());
        assertEquals("ovpn-fail", vpnTaskRepository.findById(t3.getId()).orElseThrow().getError());
    }
}


