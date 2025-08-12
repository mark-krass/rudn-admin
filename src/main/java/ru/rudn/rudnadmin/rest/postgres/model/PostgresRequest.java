package ru.rudn.rudnadmin.rest.postgres.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PostgresRequest {

    @NotNull
    @Schema(description = "Имя базы данных postgres", example = "students_db")
    private String dbName;

    @Schema(description = "SQL-запрос, применяемый к каждой схеме", example = "[\"CREATE TABLE t(id serial)\"]")
    private List<String> model;
}


