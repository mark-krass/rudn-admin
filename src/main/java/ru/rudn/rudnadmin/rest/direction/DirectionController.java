package ru.rudn.rudnadmin.rest.direction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rudn.rudnadmin.entity.Direction;
import ru.rudn.rudnadmin.rest.direction.mapper.DirectionMapper;
import ru.rudn.rudnadmin.rest.direction.service.DirectionService;
import ru.rudn.rudnadmin.rest.direction.model.DirectionDto;

import jakarta.validation.Valid;

import java.util.List;

import static org.springframework.http.ResponseEntity.*;
import static ru.rudn.rudnadmin.rest.global.utils.ExceptionHelperUtils.getEntityException;

@RestController
@RequestMapping("/api/directions")
@Tag(name = "Управление направлениями", description = "API для работы с направлениями обучения")
@RequiredArgsConstructor
public class DirectionController {

    private final DirectionService directionService;
    private final DirectionMapper directionMapper;

    @Operation(
            summary = "Создание нового направления",
            description = "Создает новое направление обучения с уникальным кодом и названием"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Направление успешно создано"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Доступ запрещен"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Непредвиденная ошибка"
            )
    })
    @PostMapping
    public ResponseEntity<Void> createDirections(
            @Parameter(description = "Данные направления для создания", required = true)
            @Valid @RequestBody final List<DirectionDto> directionsDto) {
        final List<Direction> directions = directionMapper.toEntity(directionsDto);
        directionService.saveAll(directions);

        return status(HttpStatus.CREATED).build();
    }

    @Operation(
            summary = "Получение всех направлений"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Направления успешно отправлены"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Доступ запрещен"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Непредвиденная ошибка"
            )
    })
    @GetMapping
    public ResponseEntity<List<DirectionDto>> getDirections() {
        final List<DirectionDto> directionsDto = directionMapper.toResponse(directionService.findAll());

        return ok().body(directionsDto);
    }

    @Operation(
            summary = "Получение одного направления"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Направление успешно отправлено"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Доступ запрещен"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Непредвиденная ошибка"
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<DirectionDto> getDirection(@PathVariable final Long id) {
        final Direction direction = directionService.findById(id)
                .orElseThrow(getEntityException(Direction.class));
        return ok(directionMapper.toResponse(direction));
    }

    @Operation(
            summary = "Обновление одного направления"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Обновление прошло успешно"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Доступ запрещен"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Непредвиденная ошибка"
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateDirection(@PathVariable final Long id, @Parameter(description = "Данные направления для обновления", required = true)
    @Valid @RequestBody final DirectionDto directionRequestDto) {
        final Direction direction = directionService.findById(id)
                .orElseThrow(getEntityException(Direction.class));
        directionMapper.updateEntity(directionRequestDto, direction);
        directionService.save(direction);

        return ok().build();
    }

    @Operation(
            summary = "Удаление направления",
            description = "Удаляет направление обучения по его идентификатору"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Направление успешно удалено"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Направление не найдено"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Доступ запрещен"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Непредвиденная ошибка"
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDirection(@PathVariable final Long id) {
        directionService.deleteById(id);

        return noContent().build();

    }
}
