package ru.rudn.rudnadmin.service.vpn.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rudn.rudnadmin.entity.vpn.TaskType;
import ru.rudn.rudnadmin.entity.vpn.VpnTask;
import ru.rudn.rudnadmin.repository.VpnTaskRepository;
import ru.rudn.rudnadmin.service.openvpn.VpnService;
import ru.rudn.rudnadmin.service.vpn.VpnTaskFlow;

import static ru.rudn.rudnadmin.entity.vpn.TaskType.CREATE_OVPN;
import static ru.rudn.rudnadmin.entity.vpn.TaskType.UPLOAD_MINIO;

@Component
@RequiredArgsConstructor
public class VpnTaskFlowCreateOvpn implements VpnTaskFlow {

    private final VpnService vpnService;
    private final VpnTaskRepository taskRepository;

    @Override
    public void makeLogic(VpnTask task) {
        final Long connectId = getConnectId(task);
        vpnService.createRemoteVpn(connectId);
    }

    @Override
    public void changeState(VpnTask task) {
        task.setType(UPLOAD_MINIO);
        taskRepository.save(task);
    }

    @Override
    public TaskType getType() {
        return CREATE_OVPN;
    }
}
