package ru.rudn.rudnadmin.rest.vpn;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rudn.rudnadmin.rest.global.exception.EntityNotFoundException;
import ru.rudn.rudnadmin.rest.vpn.mapper.VpnMapper;
import ru.rudn.rudnadmin.rest.vpn.model.VpnDto;
import ru.rudn.rudnadmin.rest.vpn.service.VpnService;

import java.util.List;

import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/vpn")
@Tag(name = "Управление", description = "API для управления vpn")
@RequiredArgsConstructor
public class VpnController {

    final VpnService vpnService;
    final VpnMapper vpnMapper;

    @Operation(summary = "Создание vpn для всех студентов из группы")
    @PostMapping("/createForGroup/{groupId}")
    public ResponseEntity<Void> createByGroupId(@PathVariable final Long groupId) {
        vpnService.createForGroup(groupId);
        return accepted().build();
    }

    @Operation(summary = "Создание vpn для конкретного студента")
    @PostMapping("/createForStudent/{studentId}")
    public ResponseEntity<Void> createByStudentId(@PathVariable final Long studentId) {
        vpnService.createForStudent(studentId);
        return accepted().build();
    }

    @Operation(summary = "Получить все VPN по группе")
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<VpnDto>> getByGroup(@PathVariable final Long groupId) {
        return ok(vpnMapper.toResponse(vpnService.findAllVpnStudentsByGroupId(groupId)));
    }

    @Operation(summary = "Получить VPN по id студента")
    @GetMapping("/student/{studentId}")
    public ResponseEntity<VpnDto> getByStudent(@PathVariable final Long studentId) {
        return vpnService.findVpnByStudentId(studentId)
                .map(v -> ok(vpnMapper.toResponse(v)))
                .orElseThrow(() -> new EntityNotFoundException("Cannot find such studentId!"));
    }

    @Operation(summary = "Получить новый линк на VPN по группе. Обновляется только link")
    @PutMapping("/group/{groupId}")
    public ResponseEntity<Void> renewLinkByGroup(@PathVariable final Long groupId) {
        vpnService.updateLinkByGroupId(groupId);
        return accepted().build();
    }

    @Operation(summary = "Получить новый линк на VPN по id студента. Обновляется только link")
    @PutMapping("/student/{studentId}")
    public ResponseEntity<Void> renewLinkByStudent(@PathVariable final Long studentId) {
        vpnService.updateLinkByStudentId(studentId);
        return accepted().build();
    }

    @Operation(summary = "Удалить все VPN по группе")
    @DeleteMapping("/group/{groupId}")
    public ResponseEntity<Void> deleteByGroup(@PathVariable final Long groupId) {
        vpnService.deleteByGroupId(groupId);
        return accepted().build();
    }

    @Operation(summary = "Удалить VPN по id студента")
    @DeleteMapping("/student/{studentId}")
    public ResponseEntity<Void> deleteByStudent(@PathVariable final Long studentId) {
        vpnService.deleteByStudentId(studentId);
        return accepted().build();
    }
}
