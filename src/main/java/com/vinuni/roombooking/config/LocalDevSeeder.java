package com.vinuni.roombooking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.vinuni.roombooking.model.Admin;
import com.vinuni.roombooking.model.Staff;
import com.vinuni.roombooking.model.Student;
import com.vinuni.roombooking.service.DatabaseConnector;

/**
 * Seeds three demo users (alice / bob / carol — password "pass") into the
 * H2 dev datasource so the LoginView demo accounts work end-to-end without
 * a real MySQL server.
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
        seedIfMissing(new Student("25dung.lh", "dung", "pass", "25dung.lh@vinuni.edu.vn",
                "S2024-001", "Data Science", 3));
        seedIfMissing(new Student("25tam.lh", "tam", "pass", "25tam.lh@vinuni.edu.vn",
                "S2024-001", "Data Science", 3));
        seedIfMissing(new Student("25mien.ddh", "mien", "pass", "25mien.ddh@vinuni.edu.vn",
                "S2024-001", "Computer Science", 3));
        seedIfMissing(new Staff("an.nk", "an", "pass", "an.nk@vinuni.edu.vn",
                "ST-001", "Library"));
        seedIfMissing(new Staff("dung.nq", "qdung", "pass", "dung.nq@vinuni.edu.vn",
                "ST-001", "Elab"));
        seedIfMissing(new Admin("carol", "carol", "pass", "carol@vinuni.edu.vn",
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
