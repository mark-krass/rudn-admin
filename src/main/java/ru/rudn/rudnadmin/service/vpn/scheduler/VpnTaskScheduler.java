package ru.rudn.rudnadmin.service.vpn.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.rudn.rudnadmin.entity.vpn.VpnTask;
import ru.rudn.rudnadmin.repository.VpnTaskRepository;

import java.time.LocalDateTime;
import java.util.List;


@Component
@Slf4j
@RequiredArgsConstructor
public class VpnTaskScheduler {

    private final VpnTaskRepository taskRepository;
    private final VpnTaskStepExecutor stepExecutor;

    @Scheduled(cron = "0 0 23 * * *")
    public synchronized void createVpnForAllTasks() {
        log.info("Starting creating vpn scheduler at {}", LocalDateTime.now());
        final List<VpnTask> vpnTasksByIsActive = taskRepository.findVpnTasksByIsActiveTrue();
        // Обновляется в одном потоке, т.к. операция проводится один раз в год
        for (VpnTask vpnTask : vpnTasksByIsActive) {
            final Long taskId = vpnTask.getId();
            boolean keepProcessing = true;
            while (keepProcessing) {
                try {
                    keepProcessing = stepExecutor.executeNextStep(taskId);
                } catch (Exception e) {
                    log.error("Smthg bad was happening while executing task for creation with id {}", taskId, e);
                    taskRepository.findById(taskId).ifPresentOrElse(updated -> {
                        updated.setError(e.getMessage());
                        taskRepository.save(updated);
                    }, () -> log.error("Task {} not found on error handling (probably deleted)", taskId));
                    break;
                }
            }
        }

        log.info("Vpn creating scheduler done at {}", LocalDateTime.now());
    }
}
