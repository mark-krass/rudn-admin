package ru.rudn.rudnadmin.rest.group;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rudn.rudnadmin.entity.Direction;
import ru.rudn.rudnadmin.entity.Group;
import ru.rudn.rudnadmin.rest.direction.service.DirectionService;
import ru.rudn.rudnadmin.rest.group.service.GroupService;
import ru.rudn.rudnadmin.rest.group.mapper.GroupMapper;
import ru.rudn.rudnadmin.rest.group.model.GroupDto;

import java.util.List;

import static org.springframework.http.ResponseEntity.*;
import static ru.rudn.rudnadmin.rest.global.utils.ExceptionHelperUtils.getEntityException;
import static ru.rudn.rudnadmin.rest.global.utils.ExceptionHelperUtils.getEntityParamException;

@RestController
@RequestMapping("/api/groups")
@Tag(name = "Управление группами")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final DirectionService directionService;
    private final GroupMapper mapper;

    @Operation(summary = "Создать группы")
    @ApiResponses({@ApiResponse(responseCode = "201")})
    @PostMapping
    public ResponseEntity<Void> create(
            @Parameter(required = true) @Valid @RequestBody final List<GroupDto> dtos) {

        final List<Group> groups = dtos.stream()
                .map(dto -> {
                    final Group group = mapper.toEntity(dto);
                    final Long dirId = dto.getDirectionId();
                    final Direction direction = directionService.findById(dirId)
                            .orElseThrow(getEntityParamException(Direction.class));
                    group.setDirection(direction);
                    return group;
                })
                .toList();

        groupService.saveAll(groups);
        return status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Список групп")
    @GetMapping
    public ResponseEntity<List<GroupDto>> list() {
        return ok(mapper.toResponse(groupService.findAll()));
    }

    @Operation(summary = "Получить группу")
    @GetMapping("/{id}")
    public ResponseEntity<GroupDto> get(@PathVariable final Long id) {
        final Group group = groupService.findById(id).orElseThrow(getEntityException(Group.class));

        return ok(mapper.toResponse(group));
    }

    @Operation(summary = "Обновить группу")
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable final Long id,
                                       @Parameter(required = true) @Valid @RequestBody final GroupDto groupDto) {
        final Group group = groupService.findById(id).orElseThrow(getEntityException(Group.class));
        final Direction direction = directionService.findById(groupDto.getDirectionId()).orElseThrow(() -> getEntityParamException(Direction.class).get());

        mapper.updateEntity(groupDto, group);
        group.setDirection(direction);
        groupService.save(group);

        return ok().build();
    }

    @Operation(summary = "Удалить группу")
    @ApiResponses({@ApiResponse(responseCode = "204")})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        groupService.deleteById(id);

        return noContent().build();
    }
}
