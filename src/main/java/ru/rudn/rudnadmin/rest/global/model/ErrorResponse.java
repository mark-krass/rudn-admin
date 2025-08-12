package ru.rudn.rudnadmin.rest.global.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Ошибка")
public class ErrorResponse {

    @Schema(description = "Описание ошибки")
    private String message;
}
