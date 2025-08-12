package ru.rudn.rudnadmin.rest.student;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rudn.rudnadmin.entity.Group;
import ru.rudn.rudnadmin.entity.Student;
import ru.rudn.rudnadmin.entity.User;
import ru.rudn.rudnadmin.rest.group.service.GroupService;
import ru.rudn.rudnadmin.rest.student.service.StudentService;
import ru.rudn.rudnadmin.rest.student.mapper.StudentMapper;
import ru.rudn.rudnadmin.rest.student.model.StudentUpdateDto;
import ru.rudn.rudnadmin.rest.student.model.StudentsDto;
import ru.rudn.rudnadmin.rest.user.service.UserService;

import java.util.List;

import static org.springframework.http.ResponseEntity.*;
import static ru.rudn.rudnadmin.rest.global.utils.ExceptionHelperUtils.*;

@RestController
@RequestMapping("/api/students")
@Tag(name = "Управление студентами")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final GroupService groupService;
    private final UserService userService;

    private final StudentMapper mapper;

    @Operation(summary = "Создать студентов")
    @PostMapping
    public ResponseEntity<Void> create(@Parameter(required = true) @Valid @RequestBody final StudentsDto dtos) {
        final Long groupId = dtos.getGroupId();
        final Group group = groupService.findById(groupId)
                .orElseThrow(getEntityParamException(Group.class));

        final List<Student> students = dtos.getStudentInfoDto().stream()
                .map(dto -> {
                    final Student s = mapper.toEntity(dto);
                    final Long userId = dto.getUserId();
                    final User user = userService.findById(userId)
                            .orElseThrow(getEntityParamException(User.class));
                    s.setGroup(group);
                    s.setUser(user);
                    return s;
                })
                .toList();

        studentService.saveAllStudents(students);
        return status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Список студентов")
    @GetMapping
    public ResponseEntity<List<StudentsDto>> list() {
        return ok(mapper.toResponse(studentService.findAllStudents()));
    }

    @Operation(summary = "Получить студента")
    @GetMapping("/{id}")
    public ResponseEntity<StudentsDto> get(@PathVariable final Long id) {
        final Student student = studentService.findStudentById(id).orElseThrow(getEntityException(Student.class));

        return ok(mapper.toResponse(student));
    }

    @Operation(summary = "Получить всех студентов по имени группы")
    @GetMapping("/studentsByGroupName")
    public ResponseEntity<StudentsDto> getStudentsByGroupName(@RequestParam String name) {
        final StudentsDto responseList = mapper.toStudents(studentService.findStudentsByGroup_Name(name));
        return ok().body(responseList);
    }

    @Operation(summary = "Обновить студента")
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable final Long id,
                                       @Parameter(required = true) @Valid @RequestBody final StudentUpdateDto dto) {
        final Student student = studentService.findStudentById(id).orElseThrow(getEntityException(Student.class));

        final Long groupId = dto.getGroupId();
        final Group group = groupService.findById(groupId).orElseThrow(getEntityParamException(Group.class));
        final Long userId = dto.getUserId();
        final User user = userService.findById(userId).orElseThrow(getEntityParamException(User.class));


        student.setGroup(group);
        student.setUser(user);
        studentService.saveStudent(student);

        return ok().build();
    }

    @Operation(summary = "Удалить студента")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        studentService.deleteStudentById(id);

        return noContent().build();
    }

    @Operation(summary = "Удалить студентов по группе")
    @DeleteMapping("/group/{groupId}")
    public ResponseEntity<Void> deleteByGroupId(@PathVariable final Long groupId) {
        studentService.deleteStudentsByGroupId(groupId);

        return noContent().build();
    }
}
