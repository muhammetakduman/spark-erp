package com.electrician.tracker.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class DatabasePathResolverTest {

    @Test
    void resolvesAnExistingWritableDatabaseFolder() {
        Path databaseFile = DatabasePathResolver.resolveDatabaseFile();

        assertTrue(databaseFile.getFileName().toString().endsWith(".db"));
        assertTrue(Files.isDirectory(databaseFile.getParent()));
    }
}
