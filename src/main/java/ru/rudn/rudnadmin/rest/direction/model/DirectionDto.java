package ru.rudn.rudnadmin.rest.direction.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Модель направления обучения")
public class DirectionDto {

    @Schema(description = "Уникальный идентификатор направления", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Schema(description = "Название направления", example = "Информационные технологии", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Название направления обязательно")
    private String name;
    
    @Schema(description = "Код направления", example = "09.03.01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Код направления обязателен")
    private String code;
    
    @Schema(description = "Статус активности направления", example = "true", defaultValue = "true")
    @JsonProperty("is_active")
    @Builder.Default
    private Boolean isActive = true;
}
