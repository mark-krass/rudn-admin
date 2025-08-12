package ru.rudn.rudnadmin.rest.group.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Модель ответа группы")
public class GroupDto {

    @Schema(description = "ID группы", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Schema(description = "Название группы", example = "ИКБО-01-24", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Название группы обязательно")
    private String name;

    @Schema(description = "Год набора", example = "2024", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Год обязателен")
    @Min(value = 1900, message = "Год должен быть не меньше 1900")
    @Max(value = 2200, message = "Год должен быть не больше 2200")
    private Short year;

    @Schema(description = "Идентификатор направления", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Направление обязательно")
    private Long directionId;
}
