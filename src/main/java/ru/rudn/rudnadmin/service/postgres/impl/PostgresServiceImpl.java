package ru.rudn.rudnadmin.service.postgres.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.rudn.rudnadmin.entity.Group;
import ru.rudn.rudnadmin.entity.Student;
import ru.rudn.rudnadmin.rest.student.service.StudentService;
import ru.rudn.rudnadmin.service.postgres.PostgresService;
import ru.rudn.rudnadmin.service.postgres.UserCredential;
import ru.rudn.rudnadmin.service.postgres.exception.DBCreationException;
import ru.rudn.rudnadmin.service.postgres.exception.StudentDBCreationException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ru.rudn.rudnadmin.rest.global.utils.ExceptionHelperUtils.getEntityException;
import static ru.rudn.rudnadmin.service.postgres.utils.PostgresUtils.generatePassword;
import static ru.rudn.rudnadmin.service.postgres.utils.PostgresUtils.substringEmail;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostgresServiceImpl implements PostgresService {

    private final StudentService studentService;

    @Value("${student.datasource.url}")
    private String url;

    @Value("${student.datasource.username}")
    private String rootUser;

    @Value("${student.datasource.password}")
    private String rootPass;

    @Override
    public UserCredential createForStudent(final Long studentId, final String databaseName, final List<String> modelSql) {
        final Student student = studentService.findStudentById(studentId).orElseThrow(getEntityException(Student.class));

        return createForStudents(Collections.singletonList(student), databaseName, modelSql).stream().findFirst().orElseThrow();
    }

    @Override
    public List<UserCredential> createForGroup(final Long groupId, final String databaseName, final List<String> modelSql) {
        final List<Student> students = studentService.findStudentsByGroup_Id(groupId);
        if (CollectionUtils.isEmpty(students)) throw getEntityException(Group.class).get();

        return createForStudents(students, databaseName, modelSql);
    }

    private List<UserCredential> createForStudents(final List<Student> students, final String databaseName, final List<String> modelSql) {
        final List<UserCredential> credentials = new ArrayList<>();
        // 1) проверяем/создаём базу на админ-подключении
        checkOrCreateDB(databaseName);

        // 2) работаем внутри нужной базы, одной транзакцией
        final String tenantUrl = url + databaseName;
        try (final Connection conn = DriverManager.getConnection(tenantUrl, rootUser, rootPass)) {
            conn.setAutoCommit(false);
            try (final Statement st = conn.createStatement()) {
                for (Student s : students) {
                    final String email = s.getUser().getEmail();
                    final String schema = substringEmail(email);
                    final String username = schema;
                    final String quotedUsername = quoteIdentifier(username);
                    final String quotedSchema = quoteIdentifier(schema);
                    String password = null;
                    boolean exists;
                    try (ResultSet rsRole = st.executeQuery("SELECT 1 FROM pg_roles WHERE rolname=" + quoteLiteral(username))) {
                        exists = rsRole.next();
                    }
                    if (!exists) {
                        password = generatePassword();
                        st.executeUpdate("CREATE ROLE " + quotedUsername + " LOGIN PASSWORD " + quoteLiteral(password));
                    }

                    // схема пользователя
                    st.executeUpdate("DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname=" + quoteLiteral(schema) + ") THEN CREATE SCHEMA " + quotedSchema + "; END IF; END $$;");
                    st.executeUpdate("ALTER SCHEMA " + quotedSchema + " OWNER TO " + quotedUsername + ";");

                    // доступ только к своей схеме
                    st.executeUpdate("GRANT USAGE ON SCHEMA " + quotedSchema + " TO " + quotedUsername + ";");
                    st.executeUpdate("ALTER ROLE " + quotedUsername + " SET search_path = " + quotedSchema + ";");
                    st.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE " + quotedUsername + " IN SCHEMA " + quotedSchema + " GRANT ALL ON TABLES TO " + quotedUsername + ";");
                    st.executeUpdate("ALTER DEFAULT PRIVILEGES FOR ROLE " + quotedUsername + " IN SCHEMA " + quotedSchema + " GRANT ALL ON SEQUENCES TO " + quotedUsername + ";");

                    // накатываем модель от имени самого пользователя, чтобы он стал владельцем созданных объектов
                    if (modelSql != null && !modelSql.isEmpty()) {
                        // Важно: сначала устанавливаем роль пользователя, затем search_path на его схему
                        st.executeUpdate("SET ROLE " + quotedUsername);
                        st.executeUpdate("SET search_path TO " + quotedSchema);
                        for (String sql : modelSql) {
                            if (sql != null && !sql.isBlank()) st.executeUpdate(sql);
                        }
                        // Сбрасываем search_path и роль назад к admin
                        st.executeUpdate("RESET search_path");
                        st.executeUpdate("RESET ROLE");
                    }

                    credentials.add(new UserCredential(username, password, schema));
                }
            }
            conn.commit();
        } catch (Exception e) {
            log.error("Student postgres creation failed", e);
            throw new StudentDBCreationException("Student postgres creation failed " + e.getMessage());
        }

        return credentials;
    }

    private static String quoteIdentifier(final String identifier) {
        if (identifier == null) return "\"\"";
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static String quoteLiteral(final String literal) {
        if (literal == null) return "''";
        return "'" + literal.replace("'", "''") + "'";
    }

    private void checkOrCreateDB(final String databaseName) {
        final String adminUrl = url + "postgres";
        try (final Connection connection = DriverManager.getConnection(adminUrl, rootUser, rootPass);
             final Statement st = connection.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT 1 FROM pg_database WHERE datname='" + databaseName + "'")) {
                if (!rs.next()) {
                    st.executeUpdate("CREATE DATABASE \"" + databaseName + "\"");
                }
            }
        } catch (Exception e) {
            log.error("Failed to ensure database {} exists", databaseName, e);
            throw new DBCreationException(String.format("Failed to ensure database %s exists: %s", databaseName, e.getMessage()));
        }
    }
}


