package ru.rudn.rudnadmin.service.vpn;

import ru.rudn.rudnadmin.entity.vpn.TaskType;
import ru.rudn.rudnadmin.entity.vpn.VpnTask;

/**
 * create flow: CREATE_OVPN -> UPLOAD_MINIO -> RENEW_LINK -> set active false
 * renew flow: RENEW_LINK -> set active false
 * delete flow: DELETE_MINIO -> DELETE_OVPN -> set active false
 */
public interface VpnTaskFlow {

    String VPN_EXTENSION = ".ovpn";

    default void process(VpnTask task) throws Exception {
        makeLogic(task);
        changeState(task);
    }

    void makeLogic(VpnTask task) throws Exception;

    void changeState(VpnTask task);

    TaskType getType();

    default String getVpnName(final Long connectId) {
        return connectId + VPN_EXTENSION;
    }

    default Long getConnectId(final VpnTask task) {
        return task.getVpn().getId();
    }
}
