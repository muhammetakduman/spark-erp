package com.electrician.tracker.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SingleInstanceLockTest {

    @TempDir
    Path dataFolder;

    @Test
    void secondCopyIsRefusedUntilTheFirstReleasesTheLock() throws IOException {
        Optional<SingleInstanceLock> first = SingleInstanceLock.tryAcquire(dataFolder);
        assertThat(first).isPresent();

        assertThat(SingleInstanceLock.tryAcquire(dataFolder)).isEmpty();

        first.get().release();
        Optional<SingleInstanceLock> again = SingleInstanceLock.tryAcquire(dataFolder);
        assertThat(again).isPresent();
        again.get().release();
    }

    @Test
    void createsTheDataFolderOnFirstStart() throws IOException {
        Path missing = dataFolder.resolve("SparkERP");

        Optional<SingleInstanceLock> lock = SingleInstanceLock.tryAcquire(missing);

        assertThat(lock).isPresent();
        assertThat(missing.resolve(SingleInstanceLock.LOCK_FILE_NAME)).exists();
        lock.get().release();
    }
}
