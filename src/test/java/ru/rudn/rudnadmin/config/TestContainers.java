package ru.rudn.rudnadmin.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class TestContainers {

    private static final String SCHEMA = "test_schema";

    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.9")
            .withDatabaseName("test_db")
            .withUrlParam("currentSchema", SCHEMA)
            .withUsername("test_user")
            .withPassword("test_pass")
            .withReuse(true);

    protected static final MinIOContainer MINIO = new MinIOContainer("minio/minio:RELEASE.2025-07-23T15-54-02Z")
            .withUserName("testMinioUser")
            .withPassword("testMinioPass")
            .withReuse(true);

    protected static final PostgreSQLContainer<?> STUDENT_POSTGRESQL = new PostgreSQLContainer<>("postgres:16.9")
            .withDatabaseName("postgres")
            .withUsername("tenant_root")
            .withPassword("tenant_pass");

    static {
        POSTGRES.start();
        MINIO.start();
        STUDENT_POSTGRESQL.start();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_HOST", POSTGRES::getHost);
        registry.add("DB_PORT", POSTGRES.getFirstMappedPort()::toString);
        registry.add("DB_NAME", POSTGRES::getDatabaseName);
        registry.add("DB_SCHEMA", () -> SCHEMA);
        registry.add("DB_USERNAME", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);

        registry.add("MINIO_URL", MINIO::getS3URL);
        registry.add("MINIO_ACCESS_KEY", MINIO::getUserName);
        registry.add("MINIO_SECRET_KEY", MINIO::getPassword);

        registry.add("student.datasource.url", () -> "jdbc:postgresql://" + STUDENT_POSTGRESQL.getHost() + ":" + STUDENT_POSTGRESQL.getFirstMappedPort() + "/");
        registry.add("student.datasource.username", STUDENT_POSTGRESQL::getUsername);
        registry.add("student.datasource.password", STUDENT_POSTGRESQL::getPassword);
    }
}
