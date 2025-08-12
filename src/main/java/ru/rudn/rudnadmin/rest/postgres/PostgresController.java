package ru.rudn.rudnadmin.rest.postgres;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.rudn.rudnadmin.service.postgres.impl.PostgresServiceImpl;
import ru.rudn.rudnadmin.service.postgres.UserCredential;
import ru.rudn.rudnadmin.rest.postgres.model.PostgresRequest;

import java.util.List;

import static org.springframework.http.ResponseEntity.status;

@RestController
@RequestMapping("/api/postgres")
@Tag(name = "Postgres", description = "Создание схем и пользователей в отдельной БД Postgres")
@RequiredArgsConstructor
@Validated
public class PostgresController {

    private final PostgresServiceImpl postgresService;

    @Operation(summary = "Сгенерировать схемы и пользователей по группе")
    @PostMapping("/group/{groupId}")
    public ResponseEntity<List<UserCredential>> createForGroup(@PathVariable final Long groupId,
                                                               @RequestBody PostgresRequest request) {
        final List<UserCredential> students = postgresService.createForGroup(groupId, request.getDbName(), request.getModel());
        return status(HttpStatus.CREATED).body(students);
    }

    @Operation(summary = "Сгенерировать схемы и пользователей по student")
    @PostMapping("/student/{studentId}")
    public ResponseEntity<UserCredential> createForStudent(@PathVariable final Long studentId,
                                                           @RequestBody PostgresRequest request) {
        final UserCredential student = postgresService.createForStudent(studentId, request.getDbName(), request.getModel());
        return status(HttpStatus.CREATED).body(student);
    }
}


