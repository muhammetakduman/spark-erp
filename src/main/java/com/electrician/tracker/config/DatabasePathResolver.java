package com.electrician.tracker.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves the SQLite database file location, keeping it out of the
 * installation folder (never under Program Files) so the app can write to it.
 * The folder of the old program name ("SantiyeTakip") is only read, by
 * {@link DataFolderMigration}, to copy an existing user's data over once.
 */
public final class DatabasePathResolver {

    private static final String WINDOWS_APP_FOLDER_NAME = "SparkERP";
    private static final String WINDOWS_LEGACY_FOLDER_NAME = "SantiyeTakip";
    private static final String UNIX_APP_FOLDER_NAME = ".sparkerp";
    private static final String UNIX_LEGACY_FOLDER_NAME = ".santiyetakip";
    static final String DB_FILE_NAME = "veri.db";

    private DatabasePathResolver() {
    }

    public static Path resolveDatabaseFile() {
        Path databaseFile = dataFolder().resolve(DB_FILE_NAME);
        createParentDirectoryIfMissing(databaseFile);
        return databaseFile;
    }

    /** %APPDATA%\SparkERP (not created here). */
    public static Path dataFolder() {
        return baseDirectory().resolve(isWindows() ? WINDOWS_APP_FOLDER_NAME : UNIX_APP_FOLDER_NAME);
    }

    /** %APPDATA%\SantiyeTakip: where versions before Spark ERP kept their data. */
    public static Path legacyDataFolder() {
        return baseDirectory().resolve(isWindows() ? WINDOWS_LEGACY_FOLDER_NAME : UNIX_LEGACY_FOLDER_NAME);
    }

    private static Path baseDirectory() {
        String appDataDir = isWindows() ? System.getenv("APPDATA") : null;
        return appDataDir != null ? Path.of(appDataDir) : Path.of(System.getProperty("user.home"));
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
