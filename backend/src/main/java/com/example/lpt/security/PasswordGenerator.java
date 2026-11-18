package com.example.lpt.security;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Random temporary passwords (14 chars, no look-alike characters like 0/O or 1/l/I). */
@Component
public class PasswordGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LENGTH = 14;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
