package ru.rudn.rudnadmin.rest.vpn.service;

import ru.rudn.rudnadmin.entity.vpn.Vpn;

import java.util.List;
import java.util.Optional;

public interface VpnService {

    void createForGroup(Long groupId);

    void createForStudent(Long studentId);

    List<Vpn> findAllVpnStudentsByGroupId(Long groupId);

    Optional<Vpn> findVpnByStudentId(Long studentId);

    void updateLinkByStudentId(Long studentId);

    void updateLinkByGroupId(Long groupId);

    void deleteByGroupId(Long groupId);

    void deleteByStudentId(Long studentId);
}


