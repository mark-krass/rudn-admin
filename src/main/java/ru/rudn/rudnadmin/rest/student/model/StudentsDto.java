package ru.rudn.rudnadmin.rest.student.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Модель ответа студента")
public class StudentsDto {

    @Schema(description = "ID группы", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Группа обязательна")
    private Long groupId;

    @Valid
    @Size(min = 1, message = "Список студентов обязателен и не может быть пустым")
    private List<StudentInfoDto> studentInfoDto;
}
