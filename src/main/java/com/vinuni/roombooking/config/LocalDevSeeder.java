package com.vinuni.roombooking.config;

import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.Staff;
import com.vinuni.roombooking.model.Student;
import com.vinuni.roombooking.service.DatabaseConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds three demo users (alice / bob / carol — password "pass") into the
 * H2 dev datasource so the LoginView demo accounts work end-to-end without
 * a real MySQL server. Only runs under the 'local' Spring profile.
 *
 * Idempotent: skips any user that already exists.
 */
@Component
@Profile("local")
public class LocalDevSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalDevSeeder.class);
    private final DatabaseConnector db;

    public LocalDevSeeder(DatabaseConnector db) {
        this.db = db;
    }

    @Override
    public void run(String... args) {
        seedIfMissing(new Student("u-alice", "alice", "pass", "alice@vinuni.edu.vn",
                "S2024-001", "Computer Science", 3));
        seedIfMissing(new Staff("u-bob", "bob", "pass", "bob@vinuni.edu.vn",
                "ST-001", "Library"));
        seedIfMissing(new Admin("u-carol", "carol", "pass", "carol@vinuni.edu.vn",
                "AD-001"));
    }

    private void seedIfMissing(com.vinuni.roombooking.model.User u) {
        try {
            if (db.findUserByName(u.getUserName()) != null) {
                log.info("Seed: user '{}' already present, skipping.", u.getUserName());
                return;
            }
            db.insertUser(u);
            log.info("Seed: inserted demo user '{}' ({}).", u.getUserName(), u.getUserType());
        } catch (RuntimeException ex) {
            log.warn("Seed: could not insert user '{}': {}", u.getUserName(), ex.getMessage());
        }
    }
}
