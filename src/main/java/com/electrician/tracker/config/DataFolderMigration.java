package com.electrician.tracker.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

/**
 * Copies the data of the old program name to the new data folder once, on the
 * first start of Spark ERP, before anything opens the database:
 * <ol>
 * <li>the new folder already has {@code veri.db}: nothing to do;</li>
 * <li>only the old folder has it: every file (database, {@code yedekler/},
 * settings) is <em>copied</em> — the old folder stays as a backup — and
 * {@code TASINDI.txt} is written there;</li>
 * <li>neither has it: first installation, nothing to do.</li>
 * </ol>
 * The database file is copied last under a temporary name and renamed at the
 * end, so a failed copy never leaves a {@code veri.db} behind that would be
 * taken for finished data on the next start.
 */
public final class DataFolderMigration {

    /** Written into the old folder after a successful copy. */
    public static final String MOVED_NOTE_FILE = "TASINDI.txt";
    private static final String TEMP_SUFFIX = ".tasiniyor";
    private static final DateTimeFormatter NOTE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public enum Outcome {
        NOTHING_TO_DO,
        COPIED
    }

    private final Path newFolder;
    private final Path oldFolder;

    public DataFolderMigration(Path newFolder, Path oldFolder) {
        this.newFolder = newFolder;
        this.oldFolder = oldFolder;
    }

    /** For the real %APPDATA% folders. */
    public static DataFolderMigration forUserData() {
        return new DataFolderMigration(DatabasePathResolver.dataFolder(), DatabasePathResolver.legacyDataFolder());
    }

    public Path oldFolder() {
        return oldFolder;
    }

    /** @throws IOException when the copy failed; the old folder is then untouched. */
    public Outcome run(LocalDateTime now) throws IOException {
        Path newDatabase = newFolder.resolve(DatabasePathResolver.DB_FILE_NAME);
        Path oldDatabase = oldFolder.resolve(DatabasePathResolver.DB_FILE_NAME);
        if (Files.exists(newDatabase) || !Files.isRegularFile(oldDatabase)) {
            return Outcome.NOTHING_TO_DO;
        }
        Path tempDatabase = newFolder.resolve(DatabasePathResolver.DB_FILE_NAME + TEMP_SUFFIX);
        try {
            Files.createDirectories(newFolder);
            copyEverythingButDatabase();
            Files.copy(oldDatabase, tempDatabase, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
            Files.move(tempDatabase, newDatabase, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.deleteIfExists(tempDatabase);
            throw e;
        }
        writeMovedNote(now);
        return Outcome.COPIED;
    }

    private void copyEverythingButDatabase() throws IOException {
        List<Path> sources;
        try (Stream<Path> walk = Files.walk(oldFolder)) {
            sources = walk.filter(path -> !path.equals(oldFolder)).filter(this::isCopied).toList();
        }
        for (Path source : sources) {
            Path target = newFolder.resolve(oldFolder.relativize(source).toString());
            if (Files.isDirectory(source)) {
                Files.createDirectories(target);
            } else {
                Files.createDirectories(target.getParent());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            }
        }
    }

    private boolean isCopied(Path path) {
        Path relative = oldFolder.relativize(path);
        String name = relative.toString();
        return !name.equals(DatabasePathResolver.DB_FILE_NAME) && !name.equals(MOVED_NOTE_FILE);
    }

    /** Best effort: the data is already safe in the new folder when this runs. */
    private void writeMovedNote(LocalDateTime now) {
        String note = "Bu klasördeki veriler " + now.format(NOTE_TIME) + " tarihinde Spark ERP'ye kopyalandı."
                + System.lineSeparator() + "Yeni veri klasörü: " + newFolder + System.lineSeparator()
                + "Bu klasör eski sürümün yedeği olarak bırakıldı." + System.lineSeparator();
        try {
            Files.writeString(oldFolder.resolve(MOVED_NOTE_FILE), note, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not write " + MOVED_NOTE_FILE + ": " + e.getMessage());
        }
    }
}
