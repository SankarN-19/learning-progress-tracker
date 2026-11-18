package com.example.lpt.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PasswordGeneratorTest {

    private final PasswordGenerator generator = new PasswordGenerator();

    @Test
    void generatesFourteenUnambiguousCharacters() {
        for (int i = 0; i < 200; i++) {
            assertThat(generator.generate()).matches("[A-HJ-NP-Za-km-z2-9]{14}");
        }
    }

    @Test
    void generatedPasswordsDoNotRepeat() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            seen.add(generator.generate());
        }
        assertThat(seen).hasSize(500);
    }
}
