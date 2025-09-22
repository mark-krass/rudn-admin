package ru.rudn.rudnadmin.rest.vpn.scheduler;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.rudn.rudnadmin.config.TestContainersBase;
import ru.rudn.rudnadmin.entity.vpn.TaskType;
import ru.rudn.rudnadmin.entity.vpn.Vpn;
import ru.rudn.rudnadmin.entity.vpn.VpnTask;
import ru.rudn.rudnadmin.service.minio.MinioService;
import ru.rudn.rudnadmin.service.openvpn.VpnService;
import ru.rudn.rudnadmin.service.vpn.scheduler.VpnTaskScheduler;
import ru.rudn.rudnadmin.entity.Student;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static ru.rudn.rudnadmin.testutil.TestDataUtils.*;

@SpringBootTest
@AutoConfigureMockMvc
class VpnTaskSchedulerTest extends TestContainersBase {

    @Autowired
    private VpnTaskScheduler scheduler;
    @MockitoBean
    private VpnService vpnService;
    @MockitoBean
    private MinioService minioService;

    @Test
    @SneakyThrows
    @DisplayName("Шедулер: вызывает менеджер для задач CREATE и деактивирует их")
    void createTasksFlow() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity1());
        studentRepository.save(Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(TaskType.CREATE_OVPN).build());

        doNothing().when(vpnService).createRemoteVpn(anyLong());
        doNothing().when(minioService).load(anyString(), anyString(), anyString());
        when(minioService.getLink(anyString())).thenReturn("http://minio/vpn/" + vpn.getId() + ".ovpn");

        scheduler.createVpnForAllTasks();

        verify(vpnService, atLeastOnce()).createRemoteVpn(anyLong());
        final VpnTask updated = vpnTaskRepository.findById(task.getId()).orElseThrow();
        assertFalse(updated.getIsActive());
        // создаём вторую активную таску для этого же VPN (частичный уникальный индекс позволяет, т.к. первая уже не активна)
        assertDoesNotThrow(() -> vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(TaskType.DELETE_OVPN).build()));
    }

    @Test
    @DisplayName("Шедулер: вызывает менеджер для задач DELETE, деактивирует и удаляет VPN")
    void deleteTasksFlow() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity2());
        studentRepository.save(Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(TaskType.DELETE_OVPN).build());

        // мок менеджера: при удалении задачи он физически удаляет VPN, что приводит к каскадному удалению задачи
        doNothing().when(minioService).delete(anyString());
        doNothing().when(vpnService).removeRemoteVpn(vpn.getId());

        scheduler.createVpnForAllTasks();

        verify(vpnService, atLeastOnce()).removeRemoteVpn(anyLong());
        assertTrue(vpnTaskRepository.findById(task.getId()).isEmpty(), "Задача должна быть удалена каскадно (ON DELETE CASCADE) после удаления VPN");
        assertFalse(vpnRepository.findById(vpn.getId()).isPresent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Шедулер: CREATE — одна задача успешна, у второй ошибка в менеджере")
    void createTasks_mixed_success_and_error() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user1 = userRepository.save(userEntity1());
        final var user2 = userRepository.save(userEntity2());
        studentRepository.save(Student.builder().group(group).user(user1).build());
        studentRepository.save(Student.builder().group(group).user(user2).build());
        final Vpn vpn1 = vpnRepository.save(Vpn.builder().user(user1).build());
        final Vpn vpn2 = vpnRepository.save(Vpn.builder().user(user2).build());
        final VpnTask t1 = vpnTaskRepository.save(VpnTask.builder().vpn(vpn1).type(TaskType.CREATE_OVPN).build());
        final VpnTask t2 = vpnTaskRepository.save(VpnTask.builder().vpn(vpn2).type(TaskType.CREATE_OVPN).build());

        doNothing().when(vpnService).createRemoteVpn(vpn1.getId());
        doThrow(new RuntimeException("manager-mid-error")).when(vpnService).createRemoteVpn(vpn2.getId());

        scheduler.createVpnForAllTasks();

        final VpnTask u1 = vpnTaskRepository.findById(t1.getId()).orElseThrow();
        final VpnTask u2 = vpnTaskRepository.findById(t2.getId()).orElseThrow();
        assertFalse(u1.getIsActive(), "Успешная задача CREATE должна быть деактивирована");
        assertTrue(u2.getIsActive(), "Неуспешная задача CREATE должна остаться активной");
        assertEquals("manager-mid-error", u2.getError());
    }

    @Test
    @SneakyThrows
    @DisplayName("Шедулер: DELETE — одна задача успешна (VPN удалён), у второй ошибка в менеджере")
    void deleteTasks_mixed_success_and_error() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user1 = userRepository.save(userEntity1());
        final var user2 = userRepository.save(userEntity2());
        studentRepository.save(Student.builder().group(group).user(user1).build());
        studentRepository.save(Student.builder().group(group).user(user2).build());
        final Vpn vpn1 = vpnRepository.save(Vpn.builder().user(user1).build());
        final Vpn vpn2 = vpnRepository.save(Vpn.builder().user(user2).build());
        final VpnTask t1 = vpnTaskRepository.save(VpnTask.builder().vpn(vpn1).type(TaskType.DELETE_OVPN).build());
        final VpnTask t2 = vpnTaskRepository.save(VpnTask.builder().vpn(vpn2).type(TaskType.DELETE_OVPN).build());

        // успешный сценарий удаляет VPN => задача должна исчезнуть каскадно
        doNothing().when(vpnService).removeRemoteVpn(vpn1.getId());
        // второй сценарий падает в середине удаления на удалённом сервере
        doThrow(new RuntimeException("manager-mid-error")).when(vpnService).removeRemoteVpn(vpn2.getId());

        scheduler.createVpnForAllTasks();

        assertTrue(vpnRepository.findById(vpn1.getId()).isEmpty(), "VPN 1 должен быть удалён");
        assertTrue(vpnTaskRepository.findById(t1.getId()).isEmpty(), "Задача 1 должна удалиться каскадно");

        final VpnTask u2 = vpnTaskRepository.findById(t2.getId()).orElseThrow();
        assertTrue(vpnRepository.findById(vpn2.getId()).isPresent(), "VPN 2 должен остаться");
        assertEquals("manager-mid-error", u2.getError());
    }

    @Test
    @SneakyThrows
    @DisplayName("Шедулер: сохраняет ошибку и оставляет задачу активной при ошибке CREATE")
    void createTasksErrorFlow() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity1());
        studentRepository.save(Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(TaskType.CREATE_OVPN).build());

        doThrow(new RuntimeException("boom-create")).when(vpnService).createRemoteVpn(anyLong());

        scheduler.createVpnForAllTasks();

        final VpnTask updated = vpnTaskRepository.findById(task.getId()).orElseThrow();
        assertTrue(updated.getIsActive(), "Задача должна остаться активной при ошибке выполнения");
        assertEquals("boom-create", updated.getError());
    }

    @Test
    @DisplayName("Шедулер: сохраняет ошибку при ошибке DELETE и сохраняет VPN")
    void deleteTasksErrorFlow() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity2());
        studentRepository.save(Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(TaskType.DELETE_OVPN).build());

        doThrow(new RuntimeException("boom-delete")).when(vpnService).removeRemoteVpn(anyLong());

        scheduler.createVpnForAllTasks();

        // Задача должна остаться и содержать текст ошибки; VPN не должен удаляться
        final VpnTask updated = vpnTaskRepository.findById(task.getId()).orElseThrow();
        assertEquals("boom-delete", updated.getError());
        assertTrue(vpnRepository.findById(vpn.getId()).isPresent());
    }

    @Test
    @DisplayName("Шедулер: ошибка OpenVPN.removeRemoteVpn помечает задачу ошибкой и оставляет VPN")
    void deleteTasks_openvpn_error() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity2());
        studentRepository.save(Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(TaskType.DELETE_OVPN).build());

        doThrow(new RuntimeException("openvpn-delete")).when(vpnService).removeRemoteVpn(anyLong());

        scheduler.createVpnForAllTasks();

        final VpnTask updated = vpnTaskRepository.findById(task.getId()).orElseThrow();
        assertEquals("openvpn-delete", updated.getError());
        assertTrue(vpnRepository.findById(vpn.getId()).isPresent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Шедулер: ошибка MinIO.getLink помечает задачу ошибкой и не деактивирует")
    void createTasks_minio_getLink_error() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity1());
        studentRepository.save(Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(TaskType.CREATE_OVPN).build());

        doNothing().when(vpnService).createRemoteVpn(anyLong());
        doThrow(new RuntimeException("minio-link")).when(minioService).getLink(anyString());

        scheduler.createVpnForAllTasks();

        final VpnTask updated = vpnTaskRepository.findById(task.getId()).orElseThrow();
        assertTrue(updated.getIsActive());
        assertEquals("minio-link", updated.getError());
    }

    @Test
    @DisplayName("Шедулер: вызывает менеджер для задач RENEW_LINK и после успешного завершения - закрывает таску")
    void renewTasksFlow_callsManager() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity1());
        studentRepository.save(Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(TaskType.RENEW_LINK).build());

        when(minioService.getLink(anyString())).thenReturn("http://minio/vpn/" + vpn.getId() + ".ovpn");
        scheduler.createVpnForAllTasks();
        final VpnTask updated = vpnTaskRepository.findById(task.getId()).orElseThrow();
        assertFalse(updated.getIsActive());
        assertNull(updated.getError());
    }

    @Test
    @DisplayName("Шедулер: RENEW_LINK — ошибка в менеджере сохраняется и задача остаётся активной")
    void renewTasks_error_saved_and_active() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity2());
        studentRepository.save(Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(TaskType.RENEW_LINK).build());

        doThrow(new RuntimeException("manager-renew-error")).when(minioService).getLink(anyString());

        scheduler.createVpnForAllTasks();

        final VpnTask updated = vpnTaskRepository.findById(task.getId()).orElseThrow();
        assertTrue(updated.getIsActive());
        assertEquals("manager-renew-error", updated.getError());
    }
}


