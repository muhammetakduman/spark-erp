package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class BackupServiceUrlTest {

    @Test
    void backsUpTheDatabaseFileActuallyInUse() {
        assertThat(BackupService.databaseFileOf("jdbc:sqlite:C:/Users/x/AppData/Roaming/SparkERP/veri.db?foreign_keys=on"))
                .isEqualTo(Path.of("C:/Users/x/AppData/Roaming/SparkERP/veri.db"));
        assertThat(BackupService.databaseFileOf("jdbc:sqlite:/tmp/test.db")).isEqualTo(Path.of("/tmp/test.db"));
    }
}
