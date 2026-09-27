package com.electrician.tracker.service;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.electrician.tracker.config.DatabasePathResolver;
import org.springframework.stereotype.Service;

/**
 * "YedekService" — copies the SQLite file to a backup folder (kept to at
 * most {@value #MAX_BACKUPS_KEPT} files) and restores from one of them.
 */
@Service
public class BackupService {

    private static final String BACKUP_FOLDER_SETTING_KEY = "backup.folder";
    private static final String DEFAULT_BACKUP_FOLDER_NAME = "yedekler";
    private static final String BACKUP_FILE_PREFIX = "veri_";
    private static final String BACKUP_FILE_SUFFIX = ".db";
    private static final DateTimeFormatter BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
    private static final int MAX_BACKUPS_KEPT = 30;

    private final SettingService settingService;
    private final DataSource dataSource;

    public BackupService(SettingService settingService, DataSource dataSource) {
        this.settingService = settingService;
        this.dataSource = dataSource;
    }

    public Path getBackupFolder() {
        return settingService.getValue(BACKUP_FOLDER_SETTING_KEY)
                .map(Path::of)
                .orElseGet(() -> DatabasePathResolver.resolveDatabaseFile().getParent().resolve(DEFAULT_BACKUP_FOLDER_NAME));
    }

    public void setBackupFolder(Path folder) {
        settingService.setValue(BACKUP_FOLDER_SETTING_KEY, folder.toString());
    }

    public Path backupNow() {
        Path databaseFile = DatabasePathResolver.resolveDatabaseFile();
        Path folder = getBackupFolder();
        try {
            Files.createDirectories(folder);
            Path target = folder.resolve(BACKUP_FILE_PREFIX + LocalDateTime.now().format(BACKUP_TIMESTAMP) + BACKUP_FILE_SUFFIX);
            Files.copy(databaseFile, target, StandardCopyOption.REPLACE_EXISTING);
            pruneOldBackups(folder);
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void pruneOldBackups(Path folder) throws IOException {
        List<Path> backups = listBackupsIn(folder);
        for (Path old : backups.subList(Math.min(MAX_BACKUPS_KEPT, backups.size()), backups.size())) {
            Files.deleteIfExists(old);
        }
    }

    public List<Path> listBackups() {
        return listBackupsIn(getBackupFolder());
    }

    private List<Path> listBackupsIn(Path folder) {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(folder)) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith(BACKUP_FILE_SUFFIX))
                    .sorted(Comparator.comparing(this::lastModifiedSafely).reversed())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private java.nio.file.attribute.FileTime lastModifiedSafely(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Overwrites the live database file with a backup. Closes the pooled
     * datasource first so Windows does not refuse the overwrite while the
     * file is still open; the caller must tell the user to restart the
     * application afterward, since this JVM's connection pool is now dead.
     */
    public void restoreFromBackup(Path backupFile) {
        Path databaseFile = DatabasePathResolver.resolveDatabaseFile();
        if (dataSource instanceof Closeable closeable) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // best effort; the copy below will fail loudly if the file is still locked
            }
        }
        try {
            Files.copy(backupFile, databaseFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
