package ru.jobhunter.infrastructure.persistence.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DatabaseConnectionValidator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionValidator.class);

    private final DataSource dataSource;

    public DatabaseConnectionValidator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void validateConnection() {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(3)) {
                throw new IllegalStateException("PostgreSQL connection is not valid");
            }

            log.info("PostgreSQL connection validated successfully: url={}", connection.getMetaData().getURL());
        } catch (SQLException exception) {
            log.error("PostgreSQL connection validation failed", exception);
            throw new IllegalStateException("Cannot connect to PostgreSQL", exception);
        }
    }
}
