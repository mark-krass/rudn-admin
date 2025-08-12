package ru.rudn.rudnadmin.rest.student.mapper;

import org.mapstruct.*;
import ru.rudn.rudnadmin.entity.Student;
import ru.rudn.rudnadmin.rest.student.model.StudentsDto;
import ru.rudn.rudnadmin.rest.student.model.StudentInfoDto;
import ru.rudn.rudnadmin.rest.vpn.mapper.VpnMapper;

import java.util.List;
import java.util.Collections;
import java.util.Objects;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = { VpnMapper.class })
public interface StudentMapper {


    /**
     * Маппер для создания сущностей из dto <p>
     * id - генерирует JPA <p>
     * group - находится на уровень выше в {@link StudentsDto} <p>
     * user.id - id пользователя для привящки к сущности  {@link Student} <p>
     * vpn - генерируется позже через {@link ru.rudn.rudnadmin.rest.vpn.VpnController} <p>
     * @param dto source маппинга
     * @return замапленный {@link Student}
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "user.id", source = "userId")
    Student toEntity(StudentInfoDto dto);

    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "studentInfoDto", source = "entity", qualifiedByName = "buildStudentInfoList")
    StudentsDto toResponse(Student entity);

    List<StudentsDto> toResponse(List<Student> entities);

    @Named("buildStudentInfoList")
    default List<StudentInfoDto> buildStudentInfoList(Student entity) {
        if (entity == null || entity.getUser() == null) return Collections.emptyList();
        return Collections.singletonList(toStudentInfo(entity));
    }

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "vpn", source = "user.vpn")
    StudentInfoDto toStudentInfo(Student student);

    default StudentsDto toStudents(List<Student> students) {
        if (students == null || students.isEmpty()) {
            return StudentsDto.builder()
                    .groupId(null)
                    .studentInfoDto(Collections.emptyList())
                    .build();
        }

        final Long groupId = students.get(0).getGroup().getId();

        final List<StudentInfoDto> infos = students.stream()
                .filter(Objects::nonNull)
                .map(this::toStudentInfo)
                .filter(Objects::nonNull)
                .toList();

        return StudentsDto.builder()
                .groupId(groupId)
                .studentInfoDto(infos)
                .build();
    }
}
