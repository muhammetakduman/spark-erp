package com.electrician.tracker.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DataFolderMigrationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 29, 10, 30);

    @TempDir
    Path appData;

    @Test
    void copiesTheOldDataOnceAndLeavesTheOldFolderAsBackup() throws IOException {
        Path oldFolder = oldFolderWithData();
        Path newFolder = appData.resolve("SparkERP");
        DataFolderMigration migration = new DataFolderMigration(newFolder, oldFolder);

        assertThat(migration.run(NOW)).isEqualTo(DataFolderMigration.Outcome.COPIED);

        assertThat(newFolder.resolve("veri.db")).hasContent("database");
        assertThat(newFolder.resolve("yedekler/veri_2026-09-28.db")).hasContent("backup");
        assertThat(newFolder.resolve("ayar.ini")).hasContent("setting");
        assertThat(newFolder.resolve(DataFolderMigration.MOVED_NOTE_FILE)).doesNotExist();
        assertThat(oldFolder.resolve("veri.db")).hasContent("database");
        assertThat(oldFolder.resolve(DataFolderMigration.MOVED_NOTE_FILE)).content()
                .contains("29.09.2026 10:30").contains(newFolder.toString());

        Files.writeString(newFolder.resolve("veri.db"), "used by the new version");
        assertThat(migration.run(NOW.plusDays(1))).isEqualTo(DataFolderMigration.Outcome.NOTHING_TO_DO);
        assertThat(newFolder.resolve("veri.db")).hasContent("used by the new version");
    }

    @Test
    void anEmptyNewFolderDoesNotHideTheOldData() throws IOException {
        Path oldFolder = oldFolderWithData();
        Path newFolder = Files.createDirectories(appData.resolve("SparkERP"));

        assertThat(new DataFolderMigration(newFolder, oldFolder).run(NOW))
                .isEqualTo(DataFolderMigration.Outcome.COPIED);
        assertThat(newFolder.resolve("veri.db")).hasContent("database");
    }

    @Test
    void firstInstallationHasNothingToCopy() throws IOException {
        DataFolderMigration migration = new DataFolderMigration(appData.resolve("SparkERP"),
                appData.resolve("SantiyeTakip"));

        assertThat(migration.run(NOW)).isEqualTo(DataFolderMigration.Outcome.NOTHING_TO_DO);
        assertThat(appData.resolve("SparkERP")).doesNotExist();
    }

    @Test
    void aFailedCopyLeavesNoDatabaseBehindAndTheOldFolderUntouched() throws IOException {
        Path oldFolder = oldFolderWithData();
        Path blocker = Files.writeString(appData.resolve("SparkERP"), "a file where the folder should be");

        assertThatThrownBy(() -> new DataFolderMigration(blocker, oldFolder).run(NOW)).isInstanceOf(IOException.class);
        assertThat(oldFolder.resolve(DataFolderMigration.MOVED_NOTE_FILE)).doesNotExist();
        assertThat(oldFolder.resolve("veri.db")).hasContent("database");
    }

    private Path oldFolderWithData() throws IOException {
        Path oldFolder = Files.createDirectories(appData.resolve("SantiyeTakip"));
        Files.writeString(oldFolder.resolve("veri.db"), "database");
        Files.writeString(oldFolder.resolve("ayar.ini"), "setting");
        Path backups = Files.createDirectories(oldFolder.resolve("yedekler"));
        Files.writeString(backups.resolve("veri_2026-09-28.db"), "backup");
        return oldFolder;
    }
}
