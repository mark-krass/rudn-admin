package ru.rudn.rudnadmin.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

public abstract class TestContainers {

    private static final String SCHEMA = "test_schema";
    private static final String REALM = "rudn";
    private static final String KEYCLOAK_CLIENT_ID = "rudn-admin-api";
    private static final Network KEYCLOAK_NETWORK = Network.newNetwork();

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

    protected static final PostgreSQLContainer<?> KEYCLOAK_POSTGRESQL = new PostgreSQLContainer<>("postgres:16.9")
            .withDatabaseName("keycloak")
            .withUsername("keycloak")
            .withPassword("keycloak")
            .withNetwork(KEYCLOAK_NETWORK)
            .withNetworkAliases("keycloak-db")
            .withReuse(true);

    protected static final GenericContainer<?> KEYCLOAK;

    static {
        POSTGRES.start();
        MINIO.start();
        STUDENT_POSTGRESQL.start();
        KEYCLOAK_POSTGRESQL.start();
        KEYCLOAK = new GenericContainer<>("quay.io/keycloak/keycloak:26.2.5")
                .withExposedPorts(8080)
                .withNetwork(KEYCLOAK_NETWORK)
                .withEnv("KC_DB", "postgres")
                .withEnv("KC_DB_URL_HOST", "keycloak-db")
                .withEnv("KC_DB_URL_PORT", "5432")
                .withEnv("KC_DB_URL_DATABASE", KEYCLOAK_POSTGRESQL.getDatabaseName())
                .withEnv("KC_DB_USERNAME", KEYCLOAK_POSTGRESQL.getUsername())
                .withEnv("KC_DB_PASSWORD", KEYCLOAK_POSTGRESQL.getPassword())
                .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
                .withEnv("KC_HEALTH_ENABLED", "true")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("keycloak/rudn-realm.json"),
                        "/opt/keycloak/data/import/rudn-realm.json"
                )
                .withCommand("start-dev", "--http-port=8080", "--hostname-strict=false", "--import-realm")
                .waitingFor(org.testcontainers.containers.wait.strategy.Wait.forLogMessage(".*Keycloak .* started in .*", 1))
                .withStartupTimeout(Duration.ofMinutes(3))
                .withReuse(true);
        KEYCLOAK.start();
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

        registry.add("KEYCLOAK_ISSUER_URI", () -> "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080) + "/realms/" + REALM);
        registry.add("KEYCLOAK_CLIENT_ID", () -> KEYCLOAK_CLIENT_ID);
    }
}
