package ru.rudn.rudnadmin.rest.student.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.rudn.rudnadmin.rest.vpn.model.VpnDto;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Информация студента")
public class StudentInfoDto {

    @Schema(description = "ID студента", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Schema(description = "ID пользователя", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Пользователь обязателен")
    private Long userId;

    @Schema(description = "email пользователя", example = "11541@pfur.ru", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String email;

    @Schema(description = "Vpn студента", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private VpnDto vpn;

}
