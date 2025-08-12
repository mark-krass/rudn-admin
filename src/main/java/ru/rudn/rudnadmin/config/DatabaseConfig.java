package ru.rudn.rudnadmin.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;

import static java.util.Optional.ofNullable;

/**
 * Класс конфигурации БД.
 */
@Configuration
public class DatabaseConfig {

    /**
     * Создается требуемая схема, в случае её отсутствия.
     * @return BeanPostProcessor
     */
    @Bean
    public BeanPostProcessor createSchemaBeanPostProcessor() {
        return new BeanPostProcessor() {

            @Value("${spring.liquibase.default-schema}")
            private String schemaName;

            @NotNull
            @Override
            public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
                if (bean instanceof DataSource dataSource && ofNullable(((HikariDataSource) dataSource).getJdbcUrl()).map(e -> e.contains(schemaName)).orElse(false)) {
                    try (val connection = dataSource.getConnection();
                         val st = connection.createStatement()) {
                        st.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
                    } catch (SQLException e) {
                        throw new RuntimeException("Can't find initialize schema: " + schemaName, e);
                    }
                }

                return bean;
            }
        };
    }
}
