package ru.rudn.rudnadmin.service.openvpn;

public interface VpnService {

    void createRemoteVpn(Long connectId);

    void removeRemoteVpn(Long connectId);
}
