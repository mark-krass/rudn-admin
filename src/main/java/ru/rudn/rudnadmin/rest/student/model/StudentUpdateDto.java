package ru.rudn.rudnadmin.rest.student.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Информация для обновления студента")
public class StudentUpdateDto {

    @Schema(description = "ID группы", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long groupId;

    @Schema(description = "ID пользователя", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long userId;

}
