package ru.rudn.rudnadmin.testutil;

import ru.rudn.rudnadmin.entity.Direction;
import ru.rudn.rudnadmin.entity.Group;
import ru.rudn.rudnadmin.entity.User;
import ru.rudn.rudnadmin.rest.direction.model.DirectionDto;
import ru.rudn.rudnadmin.rest.group.model.GroupDto;
import ru.rudn.rudnadmin.rest.student.model.StudentsDto;
import ru.rudn.rudnadmin.rest.student.model.StudentInfoDto;
import ru.rudn.rudnadmin.rest.user.model.UserDto;

import java.io.File;

import static java.util.Collections.singletonList;

public final class TestDataUtils {

    private TestDataUtils() {
    }

    public static DirectionDto directionDto1() {
        return DirectionDto.builder()
                .name("Информационные технологии")
                .code("09.03.01")
                .build();
    }

    public static DirectionDto directionDto2() {
        return DirectionDto.builder()
                .name("Математика")
                .code("01.03.01")
                .isActive(true)
                .build();
    }

    public static DirectionDto invalidDirectionDto() {
        return DirectionDto.builder()
                .name("")
                .code("")
                .isActive(false)
                .build();
    }

    public static Direction directionEntity1() {
        return Direction.builder()
                .name("Информационные технологии")
                .code("09.03.01")
                .isActive(true)
                .build();
    }

    public static Direction directionEntity2() {
        return Direction.builder()
                .name("Математика")
                .code("01.03.01")
                .isActive(true)
                .build();
    }

    public static GroupDto groupDto1(Long directionId) {
        return GroupDto.builder()
                .name("ИКБО-01-24")
                .year((short) 2024)
                .directionId(directionId)
                .build();
    }

    public static GroupDto groupDto2(Long directionId) {
        return GroupDto.builder()
                .name("ИКБО-02-24")
                .year((short) 2024)
                .directionId(directionId)
                .build();
    }

    public static GroupDto invalidGroupDto(Long directionId) {
        return GroupDto.builder()
                .name("")
                .year(null)
                .directionId(directionId)
                .build();
    }

    public static Group groupEntity1(Direction direction) {
        return Group.builder()
                .name("ИКБО-01-24")
                .year((short) 2024)
                .direction(direction)
                .build();
    }

    // DTO студента
    public static StudentsDto studentDto(Long groupId, Long userId) {
        return StudentsDto.builder()
                .groupId(groupId)
                .studentInfoDto(singletonList(
                        StudentInfoDto.builder()
                                .userId(userId)
                                .build()
                ))
                .build();
    }

    // DTO пользователя
    public static UserDto userDto1() {
        return UserDto.builder()
                .email("user@example.com")
                .name("John")
                .lastname("Doe")
                .build();
    }

    public static UserDto userDto2() {
        return UserDto.builder()
                .email("user2@example.com")
                .name("Jane")
                .lastname("Doe")
                .build();
    }

    public static UserDto invalidUserDto() {
        return UserDto.builder()
                .email("")
                .build();
    }

    // Сущность пользователя
    public static User userEntity1() {
        return User.builder()
                .email("stud@example.com")
                .name("John")
                .lastname("Doe")
                .build();
    }

    public static User userEntity2() {
        return User.builder()
                .email("stud2@example.com")
                .name("Jane")
                .lastname("Doe")
                .build();
    }

    public static User userEntity3() {
        return User.builder()
                .email("stud3@example.com")
                .name("Jane")
                .lastname("Doe")
                .build();
    }

    public static String getVpnFilePath() {
        return new File("src/test/resources/some-connect.ovpn").getAbsolutePath();
    }
}
