package ru.rudn.rudnadmin.rest.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rudn.rudnadmin.entity.User;
import ru.rudn.rudnadmin.rest.user.service.UserService;
import ru.rudn.rudnadmin.rest.user.mapper.UserMapper;
import ru.rudn.rudnadmin.rest.user.model.UserDto;

import java.util.List;

import static org.springframework.http.ResponseEntity.*;
import static ru.rudn.rudnadmin.rest.global.utils.ExceptionHelperUtils.getEntityException;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Управление пользователями")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @Operation(summary = "Создать пользователей")
    @PostMapping
    public ResponseEntity<Void> create(@Parameter(required = true) @Valid @RequestBody final List<UserDto> dtos) {
        final List<User> users = userMapper.toEntity(dtos);
        userService.saveAll(users);

        return status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Список пользователей")
    @GetMapping
    public ResponseEntity<List<UserDto>> list() {
        return ok(userMapper.toResponse(userService.findAll()));
    }

    @Operation(summary = "Получить пользователя")
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> get(@PathVariable final Long id) {
        final User user = userService.findById(id).orElseThrow(getEntityException(User.class));

        return ok(userMapper.toResponse(user));
    }

    @Operation(summary = "Получить всех пользователей без привязки к студенту")
    @GetMapping("/usersNoStudent")
    public ResponseEntity<List<UserDto>> getUsersWithoutStudent() {
        final List<UserDto> responseList = userMapper.toResponse(userService.findAllByStudentIsNull());

        return ok().body(responseList);
    }

    @Operation(summary = "Обновить пользователя")
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable final Long id,
                                       @Parameter(required = true) @Valid @RequestBody final UserDto dto) {
        final User user = userService.findById(id).orElseThrow(getEntityException(User.class));

        userMapper.updateEntity(dto, user);
        userService.save(user);

        return ok().build();
    }

    @Operation(summary = "Удалить пользователя")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        if (!userService.exists(id)) throw getEntityException(User.class).get();

        userService.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
