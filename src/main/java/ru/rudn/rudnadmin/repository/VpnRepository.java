package ru.rudn.rudnadmin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rudn.rudnadmin.entity.vpn.Vpn;

import java.util.List;
import java.util.Optional;

public interface VpnRepository extends JpaRepository<Vpn, Long> {

    List<Vpn> findAllByUser_Student_Group_Id(Long groupId);

    Optional<Vpn> findByUser_Student_Id(Long studentId);

}
