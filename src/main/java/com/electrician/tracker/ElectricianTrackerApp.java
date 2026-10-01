package com.electrician.tracker;

import java.awt.SplashScreen;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.electrician.tracker.config.DataFolderMigration;
import com.electrician.tracker.config.DatabasePathResolver;
import com.electrician.tracker.config.SingleInstanceLock;
import com.electrician.tracker.config.SpringConfig;
import com.electrician.tracker.service.BackupService;
import com.electrician.tracker.service.LicenseService;
import com.electrician.tracker.ui.controller.LicenseDialogController;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.SparkDialog;
import com.electrician.tracker.ui.util.StageManager;
import com.electrician.tracker.ui.util.ViewPaths;
import com.electrician.tracker.ui.util.WindowDecorations;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class ElectricianTrackerApp extends Application {

    private static final String LICENSE_TITLE_KEY = "license.title";

    private ConfigurableApplicationContext springContext;
    private SingleInstanceLock instanceLock;
    private boolean alreadyRunning;
    private DataFolderMigration migration;
    private DataFolderMigration.Outcome migrationOutcome;
    private IOException migrationFailure;

    /**
     * Copies the data of the old program name first (see
     * {@link DataFolderMigration}); if that fails nothing else starts, so the
     * program never opens an empty database in place of the user's data.
     * Nothing at all is opened while another copy of the program is running.
     */
    @Override
    public void init() {
        if (!acquireInstanceLock()) {
            return;
        }
        migration = DataFolderMigration.forUserData();
        try {
            migrationOutcome = migration.run(LocalDateTime.now());
        } catch (IOException e) {
            e.printStackTrace();
            migrationFailure = e;
            return;
        }
        Path databaseFile = DatabasePathResolver.resolveDatabaseFile();

        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", "jdbc:sqlite:" + databaseFile + "?foreign_keys=on");

        springContext = new SpringApplicationBuilder(SpringConfig.class)
                .headless(false)
                .web(WebApplicationType.NONE)
                .properties(properties)
                .run();
    }

    /**
     * Licence first (once per version), then the single application window:
     * login or first setup, then the main screen of the logged-in user.
     */
    @Override
    public void start(Stage primaryStage) {
        closeSplashScreen();
        installUncaughtExceptionAlert();
        WindowDecorations.install();
        WindowDecorations.applyIcons(primaryStage);
        if (alreadyRunning) {
            SparkDialog.info(DialogUtil.message("instance.running.title"),
                    DialogUtil.message("instance.running.detail"));
            Platform.exit();
            return;
        }
        if (migrationFailure != null) {
            SparkDialog.error(DialogUtil.message("migration.failed.title"), DialogUtil.message("migration.failed.detail",
                    String.valueOf(migrationFailure.getMessage()), migration.oldFolder().toString()));
            Platform.exit();
            return;
        }
        if (!ensureLicenseAccepted()) {
            Platform.exit();
            return;
        }
        if (migrationOutcome == DataFolderMigration.Outcome.COPIED) {
            SparkDialog.info(DialogUtil.message("migration.done.title"),
                    DialogUtil.message("migration.done.detail", migration.oldFolder().toString()));
        }
        springContext.getBean(StageManager.class).start(primaryStage);
    }

    /** {@code false} when another copy holds the data folder; a lock that cannot be created does not stop the start. */
    private boolean acquireInstanceLock() {
        try {
            Optional<SingleInstanceLock> lock = SingleInstanceLock.tryAcquire(DatabasePathResolver.dataFolder());
            alreadyRunning = lock.isEmpty();
            instanceLock = lock.orElse(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return !alreadyRunning;
    }

    /** The "loading" picture the launcher shows from the double click (jpackage -splash) until a window is ready. */
    private static void closeSplashScreen() {
        try {
            SplashScreen splash = SplashScreen.getSplashScreen();
            if (splash != null) {
                splash.close();
            }
        } catch (UnsupportedOperationException e) {
            // headless or no splash support (HeadlessException is one of these): nothing to close
        }
    }

    /** The licence must be accepted once per version before the app can be used. */
    private boolean ensureLicenseAccepted() {
        if (springContext.getBean(LicenseService.class).isAccepted()) {
            return true;
        }
        LicenseDialogController dialog = springContext.getBean(ModalStageOpener.class)
                .openAndWait(ViewPaths.LICENSE_DIALOG, LICENSE_TITLE_KEY, null);
        return dialog.isAccepted();
    }

    private void installUncaughtExceptionAlert() {
        Thread.setDefaultUncaughtExceptionHandler((thread, failure) -> {
            failure.printStackTrace();
            Platform.runLater(() -> DialogUtil.showUnexpected(failure));
        });
    }

    @Override
    public void stop() {
        if (springContext == null) {
            return;
        }
        try {
            springContext.getBean(BackupService.class).backupNow();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        springContext.close();
        releaseInstanceLock();
    }

    private void releaseInstanceLock() {
        if (instanceLock == null) {
            return;
        }
        try {
            instanceLock.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
