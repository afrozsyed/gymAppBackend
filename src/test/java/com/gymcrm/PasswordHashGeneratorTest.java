package com.gymcrm;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Run this once to generate a BCrypt hash for any password.
 * Command: mvn test -Dtest=PasswordHashGeneratorTest -pl backend
 * Copy the printed hash and paste it into your SQL UPDATE below.
 */
public class PasswordHashGeneratorTest {

    @Test
    void printHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String password = "admin123";   // <-- change this to whatever you want
        System.out.println("\n========================================");
        System.out.println("BCrypt hash for: \"" + password + "\"");
        System.out.println(encoder.encode(password));
        System.out.println("========================================\n");
    }
}
