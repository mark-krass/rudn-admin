package ru.rudn.rudnadmin.rest.vpn;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rudn.rudnadmin.service.vpn.scheduler.VpnTaskScheduler;

import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/vpn/scheduler")
@Tag(name = "Шедулер VPN", description = "Ручной запуск задач шедулера")
@RequiredArgsConstructor
public class VpnSchedulerController {

    private final VpnTaskScheduler scheduler;

    @Operation(summary = "Запустить шедулер")
    @PostMapping
    public ResponseEntity<Void> run() {
        scheduler.createVpnForAllTasks();
        return ok().build();
    }
}
