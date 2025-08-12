package ru.rudn.rudnadmin.service.vpn.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ru.rudn.rudnadmin.entity.vpn.TaskType;
import ru.rudn.rudnadmin.entity.vpn.VpnTask;
import ru.rudn.rudnadmin.repository.VpnTaskRepository;
import ru.rudn.rudnadmin.service.vpn.VpnTaskFlow;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class VpnTaskStepExecutor {

    private final Map<TaskType, VpnTaskFlow> vpnFlows;
    private final VpnTaskRepository taskRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public boolean executeNextStep(final Long taskId) throws Exception {
        final VpnTask vpnTask = taskRepository.findById(taskId).orElseThrow();
        vpnFlows.get(vpnTask.getType()).process(vpnTask);

        entityManager.flush();
        entityManager.detach(vpnTask);

        return taskRepository.findById(taskId)
                .map(VpnTask::getIsActive)
                .orElse(false);
    }
}


