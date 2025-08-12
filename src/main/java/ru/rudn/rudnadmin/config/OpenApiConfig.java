package ru.rudn.rudnadmin.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "RUDN API",
                version = "1.0",
                description = "Бекенд сервис для учета студентов, создания ВПН"
        ),
        security = { @SecurityRequirement(name = "basicAuth") }
)
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic", in = SecuritySchemeIn.HEADER)
@Configuration
public class OpenApiConfig {}
