package ru.rudn.rudnadmin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rudn.rudnadmin.entity.vpn.VpnTask;
import ru.rudn.rudnadmin.entity.vpn.TaskType;

import java.util.List;

public interface VpnTaskRepository extends JpaRepository<VpnTask, Long> {

    List<VpnTask> findVpnTasksByIsActiveTrue();

    List<VpnTask> findVpnTasksByIsActiveTrueAndTypeEquals(TaskType type);
}
