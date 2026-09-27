package com.electrician.tracker.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves the SQLite database file location, keeping it out of the
 * installation folder (never under Program Files) so the app can write to it.
 */
public final class DatabasePathResolver {

    private static final String WINDOWS_APP_FOLDER_NAME = "SantiyeTakip";
    private static final String WINDOWS_DB_FILE_NAME = "veri.db";
    private static final String UNIX_APP_FOLDER_NAME = ".santiyetakip";
    private static final String UNIX_DB_FILE_NAME = "veri.db";

    private DatabasePathResolver() {
    }

    public static Path resolveDatabaseFile() {
        Path databaseFile = isWindows() ? resolveWindowsDatabaseFile() : resolveUnixDatabaseFile();
        createParentDirectoryIfMissing(databaseFile);
        return databaseFile;
    }

    private static Path resolveWindowsDatabaseFile() {
        String appDataDir = System.getenv("APPDATA");
        Path baseDir = appDataDir != null
                ? Path.of(appDataDir)
                : Path.of(System.getProperty("user.home"));
        return baseDir.resolve(WINDOWS_APP_FOLDER_NAME).resolve(WINDOWS_DB_FILE_NAME);
    }

    private static Path resolveUnixDatabaseFile() {
        Path baseDir = Path.of(System.getProperty("user.home"));
        return baseDir.resolve(UNIX_APP_FOLDER_NAME).resolve(UNIX_DB_FILE_NAME);
    }

    private static boolean isWindows() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("win");
    }

    private static void createParentDirectoryIfMissing(Path databaseFile) {
        try {
            Files.createDirectories(databaseFile.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create database directory: " + databaseFile.getParent(), e);
        }
    }
}
