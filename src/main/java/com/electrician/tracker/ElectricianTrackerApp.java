package com.electrician.tracker;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.electrician.tracker.config.DatabasePathResolver;
import com.electrician.tracker.config.SpringConfig;
import com.electrician.tracker.service.BackupService;
import com.electrician.tracker.service.LicenseService;
import com.electrician.tracker.ui.controller.LicenseDialogController;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.ModalStageOpener;
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

    @Override
    public void init() {
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
        if (!ensureLicenseAccepted()) {
            Platform.exit();
            return;
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
