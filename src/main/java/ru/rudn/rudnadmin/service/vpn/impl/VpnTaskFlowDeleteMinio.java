package ru.rudn.rudnadmin.service.vpn.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rudn.rudnadmin.entity.vpn.TaskType;
import ru.rudn.rudnadmin.entity.vpn.VpnTask;
import ru.rudn.rudnadmin.repository.VpnTaskRepository;
import ru.rudn.rudnadmin.service.minio.MinioService;
import ru.rudn.rudnadmin.service.vpn.VpnTaskFlow;

import static ru.rudn.rudnadmin.entity.vpn.TaskType.*;

@Component
@RequiredArgsConstructor
public class VpnTaskFlowDeleteMinio implements VpnTaskFlow {

    private final MinioService minioService;
    private final VpnTaskRepository taskRepository;

    @Override
    public void makeLogic(VpnTask task) {
        final Long connectId = getConnectId(task);
        minioService.delete(connectId + VPN_EXTENSION);
    }

    @Override
    public void changeState(VpnTask task) {
        task.setType(DELETE_OVPN);
        taskRepository.save(task);
    }

    @Override
    public TaskType getType() {
        return DELETE_MINIO;
    }
}
