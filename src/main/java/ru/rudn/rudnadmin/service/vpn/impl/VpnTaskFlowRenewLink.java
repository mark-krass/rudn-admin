package ru.rudn.rudnadmin.service.vpn.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rudn.rudnadmin.entity.vpn.TaskType;
import ru.rudn.rudnadmin.entity.vpn.Vpn;
import ru.rudn.rudnadmin.entity.vpn.VpnTask;
import ru.rudn.rudnadmin.repository.VpnRepository;
import ru.rudn.rudnadmin.repository.VpnTaskRepository;
import ru.rudn.rudnadmin.service.minio.MinioService;
import ru.rudn.rudnadmin.service.vpn.VpnTaskFlow;

import static ru.rudn.rudnadmin.entity.vpn.TaskType.RENEW_LINK;

@Component
@RequiredArgsConstructor
public class VpnTaskFlowRenewLink implements VpnTaskFlow {

    private final VpnRepository vpnRepository;
    private final MinioService minioService;
    private final VpnTaskRepository taskRepository;

    @Override
    public void makeLogic(VpnTask task) {
        final Long connectId = getConnectId(task);
        final String fileName = getVpnName(connectId);
        final String vpnLink = minioService.getLink(fileName);

        final Vpn vpn = vpnRepository.findById(connectId).get();
        vpn.setLink(vpnLink);
        vpnRepository.save(vpn);
    }

    @Override
    public void changeState(VpnTask task) {
        task.setIsActive(false);
        taskRepository.save(task);
    }

    @Override
    public TaskType getType() {
        return RENEW_LINK;
    }
}
