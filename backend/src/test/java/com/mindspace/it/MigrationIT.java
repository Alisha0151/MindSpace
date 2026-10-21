package com.mindspace.it;

import com.mindspace.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** If this context starts at all, Flyway applied V1-V6 and Hibernate validated every entity against the schema. */
class MigrationIT extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("all six migrations applied successfully")
    void migrations() {
        Integer applied = jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class);
        assertEquals(6, applied);
    }

    @Test
    @DisplayName("deleting a user cascades to every owned table")
    void cascade() {
        jdbc.update("insert into users (id, email, password_hash, display_name, timezone) values "
                + "('11111111-1111-1111-1111-111111111111','c@example.test', repeat('x',60), 'C', 'UTC')");
        jdbc.update("insert into coping_strategies (user_id, title) values ('11111111-1111-1111-1111-111111111111','t')");
        jdbc.update("delete from users where id = '11111111-1111-1111-1111-111111111111'");
        assertEquals(0, jdbc.queryForObject("select count(*) from coping_strategies where user_id = '11111111-1111-1111-1111-111111111111'", Integer.class));
    }
}
