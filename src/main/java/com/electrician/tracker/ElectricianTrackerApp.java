package com.electrician.tracker;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.electrician.tracker.config.DatabasePathResolver;
import com.electrician.tracker.config.FxmlViewLoader;
import com.electrician.tracker.config.SpringConfig;
import com.electrician.tracker.service.BackupService;
import com.electrician.tracker.service.LicenseService;
import com.electrician.tracker.ui.controller.LicenseDialogController;
import com.electrician.tracker.ui.util.AppSignature;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.Stylesheets;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class ElectricianTrackerApp extends Application {

    private static final String LICENSE_DIALOG_FXML = "/fxml/license_dialog.fxml";
    private static final String LICENSE_TITLE_KEY = "license.title";
    private static final String MAIN_VIEW_FXML = "/fxml/main.fxml";

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

    @Override
    public void start(Stage primaryStage) {
        installUncaughtExceptionAlert();
        if (!ensureLicenseAccepted()) {
            Platform.exit();
            return;
        }
        FxmlViewLoader fxmlViewLoader = springContext.getBean(FxmlViewLoader.class);
        Parent root = fxmlViewLoader.load(MAIN_VIEW_FXML);

        Scene scene = new Scene(root, 1024, 720);
        Stylesheets.apply(scene);

        primaryStage.setTitle(springContext.getBean(AppSignature.class).windowTitle());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /** The licence must be accepted once per version before the app can be used. */
    private boolean ensureLicenseAccepted() {
        if (springContext.getBean(LicenseService.class).isAccepted()) {
            return true;
        }
        LicenseDialogController dialog = springContext.getBean(ModalStageOpener.class)
                .openAndWait(LICENSE_DIALOG_FXML, LICENSE_TITLE_KEY, null);
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
