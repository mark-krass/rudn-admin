package ru.rudn.rudnadmin.service.postgres;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserCredential {

    @Schema(description = "Имя пользователя в postgres-БД")
    private String username;

    @Schema(description = "Сгенерированный пароль")
    private String password;
    
    @Schema(description = "Схема, принадлежащая пользователю")
    private String schema;
}


