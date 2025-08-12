package ru.rudn.rudnadmin.rest.vpn.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.rudn.rudnadmin.config.TestContainersBase;
import ru.rudn.rudnadmin.entity.Student;
import ru.rudn.rudnadmin.entity.vpn.TaskType;
import ru.rudn.rudnadmin.entity.vpn.Vpn;
import ru.rudn.rudnadmin.entity.vpn.VpnTask;
import ru.rudn.rudnadmin.service.minio.MinioService;
import ru.rudn.rudnadmin.service.openvpn.VpnService;
import ru.rudn.rudnadmin.service.vpn.scheduler.VpnTaskStepExecutor;

import static org.junit.jupiter.api.Assertions.*;
import static ru.rudn.rudnadmin.testutil.TestDataUtils.*;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
class VpnTaskStepExecutorIT extends TestContainersBase {

    @Autowired
    private VpnTaskStepExecutor stepExecutor;
    @MockitoBean
    private VpnService vpnService;
    @MockitoBean
    private MinioService minioService;

    @Test
    @DisplayName("Executor: DELETE_OVPN — после шага задача пропадает (каскад видно сразу)")
    void executor_deleteOvpn_seesCascadeImmediately() throws Exception {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity2());
        final Student student = studentRepository.save(Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(TaskType.DELETE_MINIO).build());

        // шаг DELETE_MINIO
        doNothing().when(minioService).delete(String.valueOf(vpn.getId()));
        assertTrue(stepExecutor.executeNextStep(task.getId()));

        // шаг DELETE_OVPN
        doNothing().when(vpnService).removeRemoteVpn(vpn.getId());
        final boolean keep = stepExecutor.executeNextStep(task.getId());
        assertFalse(keep, "После удаления VPN задача должна отсутствовать и цикл завершиться");

        assertTrue(vpnRepository.findById(vpn.getId()).isEmpty());
        assertTrue(vpnTaskRepository.findById(task.getId()).isEmpty());
    }

    @Test
    @DisplayName("Executor: RENEW_LINK — завершает задачу (возвращает false)")
    void executor_renew_finishesTask() throws Exception {
        final var dir = directionRepository.save(directionEntity1());
        final var group = groupRepository.save(groupEntity1(dir));
        final var user = userRepository.save(userEntity1());
        final Student student = studentRepository.save(Student.builder().group(group).user(user).build());
        final Vpn vpn = vpnRepository.save(Vpn.builder().user(user).build());
        final VpnTask task = vpnTaskRepository.save(VpnTask.builder().vpn(vpn).type(TaskType.RENEW_LINK).build());

        doNothing().when(minioService).load(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        org.mockito.Mockito.when(minioService.getLink(org.mockito.ArgumentMatchers.anyString())).thenReturn("http://minio/vpn/" + vpn.getId() + ".ovpn");

        final boolean keep = stepExecutor.executeNextStep(task.getId());
        assertFalse(keep, "RENEW_LINK должен закрыть задачу и вернуть false");
        assertFalse(vpnTaskRepository.findById(task.getId()).orElseThrow().getIsActive());
    }
}


