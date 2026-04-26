package net.ljcomputing.camelpoc.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class DuckDbDAO {
    private final JdbcTemplate jdbcTemplate;

    public DuckDbDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    private void init() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS test (id INTEGER PRIMARY KEY, name VARCHAR)");
        jdbcTemplate.execute("INSERT OR IGNORE INTO test (id, name) VALUES (1, 'Alice')");
        jdbcTemplate.execute("INSERT OR IGNORE INTO test (id, name) VALUES (2, 'Bob')");
    }

    public String getNameById(int id) {
        return jdbcTemplate.queryForObject(
            "SELECT name FROM test WHERE id = ?", String.class, id);
    }
}
