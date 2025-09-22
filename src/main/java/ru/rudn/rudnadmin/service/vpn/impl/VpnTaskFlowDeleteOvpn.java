package ru.rudn.rudnadmin.service.vpn.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rudn.rudnadmin.entity.vpn.TaskType;
import ru.rudn.rudnadmin.entity.vpn.VpnTask;
import ru.rudn.rudnadmin.repository.VpnRepository;
import ru.rudn.rudnadmin.service.openvpn.VpnService;
import ru.rudn.rudnadmin.service.vpn.VpnTaskFlow;

import static ru.rudn.rudnadmin.entity.vpn.TaskType.DELETE_OVPN;

@Component
@RequiredArgsConstructor
public class VpnTaskFlowDeleteOvpn implements VpnTaskFlow {

    private final VpnService vpnService;
    private final VpnRepository vpnRepository;

    @Override
    public void makeLogic(VpnTask task) {
        final Long connectId = getConnectId(task);

        vpnService.removeRemoteVpn(connectId);
        vpnRepository.deleteById(connectId);
    }

    @Override
    public void changeState(VpnTask task) {
        // nothing, because task already deleted by cascade
    }

    @Override
    public TaskType getType() {
        return DELETE_OVPN;
    }
}
