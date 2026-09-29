package com.electrician.tracker;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.electrician.tracker.config.DataFolderMigration;
import com.electrician.tracker.config.DatabasePathResolver;
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
    private DataFolderMigration migration;
    private DataFolderMigration.Outcome migrationOutcome;
    private IOException migrationFailure;

    /**
     * Copies the data of the old program name first (see
     * {@link DataFolderMigration}); if that fails nothing else starts, so the
     * program never opens an empty database in place of the user's data.
     */
    @Override
    public void init() {
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
        installUncaughtExceptionAlert();
        WindowDecorations.install();
        WindowDecorations.applyIcons(primaryStage);
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
    }

    public static void main(String[] args) {
        launch(args);
    }
}
