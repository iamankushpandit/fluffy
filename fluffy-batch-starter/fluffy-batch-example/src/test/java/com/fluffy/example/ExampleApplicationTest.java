package com.fluffy.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ExampleApplicationTest {

    @Test
    void contextLoads() {
        // Verifies that the entire Spring context starts up successfully
    }

    @Test
    void mainMethodRunsSuccessfully() {
        // Test that the main method can be called without error
        // (Spring context is already started by @SpringBootTest)
        ExampleApplication.main(new String[]{});
    }
}
