package ru.rudn.rudnadmin.rest.vpn.scheduler;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.rudn.rudnadmin.config.TestContainersBase;
import ru.rudn.rudnadmin.entity.vpn.TaskType;
import ru.rudn.rudnadmin.entity.vpn.Vpn;
import ru.rudn.rudnadmin.entity.vpn.VpnTask;
import ru.rudn.rudnadmin.service.minio.MinioService;
import ru.rudn.rudnadmin.service.openvpn.VpnService;
import ru.rudn.rudnadmin.service.vpn.scheduler.VpnTaskScheduler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ru.rudn.rudnadmin.entity.vpn.TaskType.*;
import static ru.rudn.rudnadmin.testutil.TestDataUtils.*;

@SpringBootTest
class VpnTaskSchedulerFlowsTest extends TestContainersBase {

    @Autowired
    private VpnTaskScheduler scheduler;

    @MockitoBean
    private VpnService vpnService;
    @MockitoBean
    private MinioService minioService;

    @Test
    @SneakyThrows
    @DisplayName("CREATE flow: success (create → upload → renew) closes task")
    void create_success_chain() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity1());
        studentRepository.save(ru.rudn.rudnadmin.entity.Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(CREATE_OVPN).build());

        doNothing().when(vpnService).createRemoteVpn(vpn.getId());
        doNothing().when(minioService).load(anyString(), anyString(), anyString());
        when(minioService.getLink(anyString())).thenReturn("http://minio/vpn/" + vpn.getId() + ".ovpn");

        scheduler.createVpnForAllTasks();

        final VpnTask updated = vpnTaskRepository.findById(task.getId()).orElseThrow();
        assertFalse(updated.getIsActive());
        assertNotNull(vpnRepository.findById(vpn.getId()).orElseThrow().getLink());
    }

    @Test
    @SneakyThrows
    @DisplayName("CREATE flow: error on OpenVPN step keeps task active and saves error")
    void create_error_on_openvpn() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity1());
        studentRepository.save(ru.rudn.rudnadmin.entity.Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(CREATE_OVPN).build());

        doThrow(new RuntimeException("boom-create")).when(vpnService).createRemoteVpn(vpn.getId());

        scheduler.createVpnForAllTasks();

        final VpnTask updated = vpnTaskRepository.findById(task.getId()).orElseThrow();
        assertEquals(updated.getType(), CREATE_OVPN);
        assertTrue(updated.getIsActive());
        assertEquals("boom-create", updated.getError());
    }

    @Test
    @SneakyThrows
    @DisplayName("CREATE flow: error on MinIO.load keeps task active and saves error")
    void create_error_on_minio_load() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity1());
        studentRepository.save(ru.rudn.rudnadmin.entity.Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(CREATE_OVPN).build());

        doNothing().when(vpnService).createRemoteVpn(vpn.getId());
        doThrow(new RuntimeException("minio-load")).when(minioService).load(anyString(), anyString(), anyString());

        scheduler.createVpnForAllTasks();

        final VpnTask updated = vpnTaskRepository.findById(task.getId()).orElseThrow();
        assertEquals(updated.getType(), UPLOAD_MINIO);
        assertTrue(updated.getIsActive());
        assertEquals("minio-load", updated.getError());
    }

    @Test
    @SneakyThrows
    @DisplayName("CREATE flow: error on MinIO.getLink keeps task active and saves error")
    void create_error_on_minio_getLink() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity1());
        studentRepository.save(ru.rudn.rudnadmin.entity.Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(CREATE_OVPN).build());

        doNothing().when(vpnService).createRemoteVpn(vpn.getId());
        doNothing().when(minioService).load(anyString(), anyString(), anyString());
        doThrow(new RuntimeException("minio-get-link")).when(minioService).getLink(anyString());

        scheduler.createVpnForAllTasks();

        final VpnTask updated = vpnTaskRepository.findById(task.getId()).orElseThrow();
        assertEquals(updated.getType(), RENEW_LINK);
        assertTrue(updated.getIsActive());
        assertEquals("minio-get-link", updated.getError());
    }

    @Test
    @DisplayName("RENEW flow: success closes task and updates link")
    void renew_success() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity1());
        studentRepository.save(ru.rudn.rudnadmin.entity.Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(TaskType.RENEW_LINK).build());

        when(minioService.getLink(anyString())).thenReturn("http://minio/vpn/" + vpn.getId() + ".ovpn");

        scheduler.createVpnForAllTasks();

        final VpnTask updated = vpnTaskRepository.findById(task.getId()).orElseThrow();
        assertEquals(updated.getType(), RENEW_LINK);
        assertFalse(updated.getIsActive());
        assertNotNull(vpnRepository.findById(vpn.getId()).orElseThrow().getLink());
    }

    @Test
    @DisplayName("RENEW flow: error keeps task active and saves error")
    void renew_error_on_getLink() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity1());
        studentRepository.save(ru.rudn.rudnadmin.entity.Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(TaskType.RENEW_LINK).build());

        doThrow(new RuntimeException("renew-link")).when(minioService).getLink(anyString());

        scheduler.createVpnForAllTasks();

        final VpnTask updated = vpnTaskRepository.findById(task.getId()).orElseThrow();
        assertEquals(updated.getType(), RENEW_LINK);
        assertTrue(updated.getIsActive());
        assertEquals("renew-link", updated.getError());
    }

    @Test
    @DisplayName("DELETE flow: success (delete minio → remove ovpn) removes VPN and task")
    void delete_success_chain() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity2());
        studentRepository.save(ru.rudn.rudnadmin.entity.Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(TaskType.DELETE_MINIO).build());

        doNothing().when(minioService).delete(anyString());
        doNothing().when(vpnService).removeRemoteVpn(vpn.getId());

        scheduler.createVpnForAllTasks();

        assertTrue(vpnRepository.findById(vpn.getId()).isEmpty());
        assertTrue(vpnTaskRepository.findById(task.getId()).isEmpty());
    }

    @Test
    @DisplayName("DELETE flow: error on MinIO.delete keeps task with error and VPN exists")
    void delete_error_on_minio_delete() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity2());
        studentRepository.save(ru.rudn.rudnadmin.entity.Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(TaskType.DELETE_MINIO).build());

        doThrow(new RuntimeException("minio-delete")).when(minioService).delete(anyString());

        scheduler.createVpnForAllTasks();

        final VpnTask updated = vpnTaskRepository.findById(task.getId()).orElseThrow();
        assertEquals(updated.getType(), DELETE_MINIO);
        assertEquals("minio-delete", updated.getError());
        assertTrue(vpnRepository.findById(vpn.getId()).isPresent());
    }

    @Test
    @DisplayName("DELETE flow: error on OpenVPN.remove keeps task with error and VPN exists")
    void delete_error_on_openvpn_remove() {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity2());
        studentRepository.save(ru.rudn.rudnadmin.entity.Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(TaskType.DELETE_MINIO).build());

        doNothing().when(minioService).delete(anyString());
        doThrow(new RuntimeException("openvpn-remove")).when(vpnService).removeRemoteVpn(vpn.getId());

        scheduler.createVpnForAllTasks();

        final VpnTask updated = vpnTaskRepository.findById(task.getId()).orElseThrow();
        assertEquals(updated.getType(), DELETE_OVPN);
        assertEquals("openvpn-remove", updated.getError());
        assertTrue(vpnRepository.findById(vpn.getId()).isPresent());
    }
}


