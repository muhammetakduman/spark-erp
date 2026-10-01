package com.electrician.tracker.config;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Keeps a second copy of the program from opening the same data folder (a
 * double click while the first copy is still starting is the usual cause):
 * the running copy holds a lock on a file in the folder. Windows releases the
 * lock when the process ends, even after a crash, so it never goes stale.
 */
public final class SingleInstanceLock {

    static final String LOCK_FILE_NAME = "calisiyor.kilit";

    private final FileChannel channel;
    private final FileLock lock;

    private SingleInstanceLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    /** Empty when another copy already holds the lock. */
    public static Optional<SingleInstanceLock> tryAcquire(Path dataFolder) throws IOException {
        Files.createDirectories(dataFolder);
        FileChannel channel = FileChannel.open(dataFolder.resolve(LOCK_FILE_NAME), StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
        FileLock lock;
        try {
            lock = channel.tryLock();
        } catch (OverlappingFileLockException e) {
            lock = null;
        }
        if (lock == null) {
            channel.close();
            return Optional.empty();
        }
        return Optional.of(new SingleInstanceLock(channel, lock));
    }

    public void release() throws IOException {
        lock.release();
        channel.close();
    }
}
