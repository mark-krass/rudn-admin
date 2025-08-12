package ru.rudn.rudnadmin.rest.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ru.rudn.rudnadmin.config.TestContainersBase;
import ru.rudn.rudnadmin.entity.Direction;
import ru.rudn.rudnadmin.entity.Group;
import ru.rudn.rudnadmin.entity.Student;
import ru.rudn.rudnadmin.entity.User;
import ru.rudn.rudnadmin.rest.postgres.model.PostgresRequest;
import ru.rudn.rudnadmin.service.postgres.UserCredential;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "test", roles = {"ADMIN"})
class PostgresControllerIT extends TestContainersBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private boolean schemaExists(String dbName, String schema) throws Exception {
        try (Connection c = DriverManager.getConnection(studentDbUrl(dbName), STUDENT_POSTGRESQL.getUsername(), STUDENT_POSTGRESQL.getPassword());
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1 FROM pg_namespace WHERE nspname='" + schema + "'")) {
            return rs.next();
        }
    }

    @Test
    @DisplayName("PostgresController: схемы создаются; доступ только к своей схеме")
    void createForGroup_success_permissions() throws Exception {
        final Direction dir = directionRepository.save(Direction.builder().name("D").code("C1").build());
        final Group group = groupRepository.save(Group.builder().name("G1").year((short) 2024).direction(dir).build());
        final User u1 = userRepository.save(User.builder().email("123@e").build());
        final User u2 = userRepository.save(User.builder().email("124@e").build());
        final Student s1 = studentRepository.save(Student.builder().group(group).user(u1).build());
        final Student s2 = studentRepository.save(Student.builder().group(group).user(u2).build());

        final String dbName = "stud_" + UUID.randomUUID().toString().replace('-', '_');
        final PostgresRequest req = new PostgresRequest();
        req.setDbName(dbName);
        req.setModel(List.of("CREATE TABLE IF NOT EXISTS t1(id serial primary key)"));

        final String json = objectMapper.writeValueAsString(req);
        final var mvcRes = mockMvc.perform(post("/api/postgres/group/" + group.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        final List<UserCredential> creds = objectMapper.readValue(mvcRes.getResponse().getContentAsByteArray(), new TypeReference<>() {});
        assertEquals(2, creds.size());
        final UserCredential c1 = creds.stream().filter(c -> c.getUsername().startsWith("123")).findFirst().orElse(creds.get(0));
        final UserCredential c2 = creds.stream().filter(c -> c.getUsername().startsWith("124")).findFirst().orElse(creds.get(1));

        assertTrue(schemaExists(dbName, c1.getSchema()));
        assertTrue(schemaExists(dbName, c2.getSchema()));

        // доступ в своей схеме
        try (Connection as1 = DriverManager.getConnection(studentDbUrl(dbName), c1.getUsername(), c1.getPassword());
             Statement st = as1.createStatement()) {
            st.executeUpdate("SET search_path TO '" + c1.getSchema() + "'");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS my_tbl(id serial)");
        }

        // попытка создать объект в чужой схеме
        try (Connection as1 = DriverManager.getConnection(studentDbUrl(dbName), c1.getUsername(), c1.getPassword());
             Statement st = as1.createStatement()) {
            assertThrows(Exception.class, () -> st.executeUpdate("CREATE TABLE " + c2.getSchema() + ".forbidden(id int)"));
        }

        // попытка прочитать таблицу из чужой схемы — должно падать по правам
        try (Connection as1 = DriverManager.getConnection(studentDbUrl(dbName), c1.getUsername(), c1.getPassword());
             Statement st = as1.createStatement()) {
            assertThrows(Exception.class, () -> {
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM " + c2.getSchema() + ".t1")) {
                    // no-op
                }
            });
        }
    }

    @Test
    @DisplayName("PostgresController: ошибка на втором пользователе откатывает транзакцию для всех")
    void createForGroup_error_atomic() throws Exception {
        final Direction dir = directionRepository.save(Direction.builder().name("D").code("C2").build());
        final Group group = groupRepository.save(Group.builder().name("G2").year((short) 2024).direction(dir).build());
        final User u1 = userRepository.save(User.builder().email("567@e").build());
        final User u2 = userRepository.save(User.builder().email("577@e").build());
        studentRepository.save(Student.builder().group(group).user(u1).build());
        studentRepository.save(Student.builder().group(group).user(u2).build());

        final String dbName = "stud_" + UUID.randomUUID().toString().replace('-', '_');
        final PostgresRequest req = new PostgresRequest();
        req.setDbName(dbName);
        // делаем фатальную ошибку (деление на ноль) — должно откатить всех
        req.setModel(List.of("CREATE TABLE IF NOT EXISTS ok1(id int)", "SELECT 1/0"));

        final String json = objectMapper.writeValueAsString(req);
        mockMvc.perform(post("/api/postgres/group/" + group.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is5xxServerError());

        // после ошибки ни одной пользовательской схемы быть не должно
        try (Connection c = DriverManager.getConnection(studentDbUrl(dbName), STUDENT_POSTGRESQL.getUsername(), STUDENT_POSTGRESQL.getPassword());
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM pg_namespace WHERE nspname like 'a%' or nspname like 'b%'")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("PostgresController: если базы нет — создаётся; если есть — idempotent")
    void database_creation_idempotent() throws Exception {
        final Direction dir = directionRepository.save(Direction.builder().name("D").code("C3").build());
        final Group group = groupRepository.save(Group.builder().name("G3").year((short) 2024).direction(dir).build());
        final User u1 = userRepository.save(User.builder().email("899@e").build());
        studentRepository.save(Student.builder().group(group).user(u1).build());

        final String dbName = "stud_" + UUID.randomUUID().toString().replace('-', '_');
        final PostgresRequest req = new PostgresRequest();
        req.setDbName(dbName);

        final String json = objectMapper.writeValueAsString(req);
        mockMvc.perform(post("/api/postgres/group/" + group.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        // повторный вызов с той же БД
        mockMvc.perform(post("/api/postgres/group/" + group.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PostgresController: повторный вызов обновляет схему (вторая таблица)")
    void reprovision_updates_schema() throws Exception {
        final Direction dir = directionRepository.save(Direction.builder().name("D").code("C4").build());
        final Group group = groupRepository.save(Group.builder().name("G4").year((short) 2024).direction(dir).build());
        final User u1 = userRepository.save(User.builder().email("115@e").build());
        final User u2 = userRepository.save(User.builder().email("116@e").build());
        studentRepository.save(Student.builder().group(group).user(u1).build());
        studentRepository.save(Student.builder().group(group).user(u2).build());

        final String dbName = "stud_" + UUID.randomUUID().toString().replace('-', '_');
        final PostgresRequest req1 = new PostgresRequest();
        req1.setDbName(dbName);
        req1.setModel(List.of("CREATE TABLE IF NOT EXISTS t1(id serial)"));

        final String json1 = objectMapper.writeValueAsString(req1);
        final var res1 = mockMvc.perform(post("/api/postgres/group/" + group.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json1))
                .andExpect(status().isCreated())
                .andReturn();

        final List<UserCredential> creds = objectMapper.readValue(res1.getResponse().getContentAsByteArray(), new TypeReference<>() {});
        final UserCredential c1 = creds.get(0);
        assertNotNull(c1.getPassword(), "Первый вызов должен вернуть сгенерированный пароль");

        // второй вызов с новой таблицей
        final PostgresRequest req2 = new PostgresRequest();
        req2.setDbName(dbName);
        req2.setModel(List.of("CREATE TABLE IF NOT EXISTS t2(id serial)"));
        final String json2 = objectMapper.writeValueAsString(req2);
        final var res2 = mockMvc.perform(post("/api/postgres/group/" + group.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json2))
                .andExpect(status().isCreated())
                .andReturn();

        final List<UserCredential> creds2 = objectMapper.readValue(res2.getResponse().getContentAsByteArray(), new TypeReference<>() {});
        final UserCredential c1second = creds2.stream().filter(u -> u.getUsername().equals(c1.getUsername())).findFirst().orElseThrow();
        assertNull(c1second.getPassword(), "Повторный вызов не должен возвращать пароль для уже существующего пользователя");

        // проверяем, что как пользователь можно увидеть t2 в своей схеме
        try (Connection as1 = DriverManager.getConnection(studentDbUrl(dbName), c1.getUsername(), c1.getPassword());
             Statement st = as1.createStatement();
             ResultSet rs = st.executeQuery("SELECT to_regclass('" + c1.getSchema() + ".t2')")) {
            assertTrue(rs.next());
            assertNotNull(rs.getString(1));
        }
    }

    @Test
    @DisplayName("PostgresController: пароль выдаётся при создании (student), при повторе пароль не возвращается; старый пароль работает")
    void student_endpoint_password_lifecycle() throws Exception {
        final Direction dir = directionRepository.save(Direction.builder().name("D").code("C5").build());
        final Group group = groupRepository.save(Group.builder().name("G5").year((short) 2024).direction(dir).build());
        final User u1 = userRepository.save(User.builder().email("651@e").build());
        final Student s1 = studentRepository.save(Student.builder().group(group).user(u1).build());

        final String dbName = "stud_" + UUID.randomUUID().toString().replace('-', '_');
        final PostgresRequest req1 = new PostgresRequest();
        req1.setDbName(dbName);
        req1.setModel(List.of("CREATE TABLE IF NOT EXISTS solo_t1(id serial)"));

        final String json1 = objectMapper.writeValueAsString(req1);
        final var res1 = mockMvc.perform(post("/api/postgres/student/" + s1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json1))
                .andExpect(status().isCreated())
                .andReturn();

        final UserCredential cred1 = objectMapper.readValue(res1.getResponse().getContentAsByteArray(), UserCredential.class);
        assertNotNull(cred1.getPassword(), "Первый вызов student должен вернуть пароль");

        // Повторный вызов без изменения пароля
        final var res2 = mockMvc.perform(post("/api/postgres/student/" + s1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json1))
                .andExpect(status().isCreated())
                .andReturn();
        final UserCredential cred2 = objectMapper.readValue(res2.getResponse().getContentAsByteArray(), UserCredential.class);
        assertNull(cred2.getPassword(), "Повторный вызов student не должен возвращать пароль");

        // Старый пароль продолжает работать
        try (Connection as1 = DriverManager.getConnection(studentDbUrl(dbName), cred1.getUsername(), cred1.getPassword());
             Statement st = as1.createStatement();
             ResultSet rs = st.executeQuery("SELECT to_regclass('" + cred1.getSchema() + ".solo_t1')")) {
            assertTrue(rs.next());
        }
    }

    private String studentDbUrl(String dbName) {
        return "jdbc:postgresql://" + STUDENT_POSTGRESQL.getHost() + ":" + STUDENT_POSTGRESQL.getFirstMappedPort() + "/" + dbName;
    }
}


