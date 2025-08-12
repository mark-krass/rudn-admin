package ru.rudn.rudnadmin.service.postgres;

import java.util.List;

public interface PostgresService {

    List<UserCredential> createForGroup(final Long groupId, final String databaseName, final List<String> modelSql);

    UserCredential createForStudent(final Long studentId, final String databaseName, final List<String> modelSql);
}
