package com.electrician.tracker.ui.util;

import java.util.ArrayList;
import java.util.List;

import com.electrician.tracker.config.FxmlViewLoader;
import com.electrician.tracker.service.AuthenticationService;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/**
 * The one application window: it shows the login (or first setup) screen,
 * then the main screen built for the logged-in user's role. Switching user
 * never closes the program: dialogs are closed, the shown screen and its data
 * are dropped, the session ends and the login screen comes back; the next
 * login builds a fresh main screen (menu and rights of the new role).
 */
@Component
public class StageManager {

    public static final KeyCombination SWITCH_USER_KEYS =
            new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
    private static final double LOGIN_WIDTH = 760;
    private static final double LOGIN_HEIGHT = 460;
    private static final double SETUP_HEIGHT = 580;
    private static final double MAIN_MIN_WIDTH = 1024;
    private static final double MAIN_MIN_HEIGHT = 700;
    private static final double MAIN_WIDTH = 1280;
    private static final double MAIN_HEIGHT = 800;

    private final FxmlViewLoader fxmlViewLoader;
    private final AuthenticationService authenticationService;
    private final ContentNavigator contentNavigator;
    private final PendingJobsBadge pendingJobsBadge;
    private final AppSignature appSignature;
    private Stage stage;

    public StageManager(FxmlViewLoader fxmlViewLoader, AuthenticationService authenticationService,
            ContentNavigator contentNavigator, PendingJobsBadge pendingJobsBadge, AppSignature appSignature) {
        this.fxmlViewLoader = fxmlViewLoader;
        this.authenticationService = authenticationService;
        this.contentNavigator = contentNavigator;
        this.pendingJobsBadge = pendingJobsBadge;
        this.appSignature = appSignature;
    }

    /** First screen: the first-run setup while no user exists, the login otherwise. */
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        showEntryScreen();
        stage.show();
    }

    /** Called by the login and setup screens once a user is logged in. */
    public void onLoggedIn() {
        showMain();
    }

    /**
     * "Kullanıcı Değiştir" (Ctrl+Shift+L): asks first if something is not
     * saved, then goes back to the login screen without closing the program.
     */
    public void switchUser() {
        if (!contentNavigator.confirmLeavingCurrent()) {
            return;
        }
        closeOtherWindows();
        contentNavigator.clear();
        pendingJobsBadge.reset();
        authenticationService.logout();
        showEntryScreen();
    }

    /** "Çıkış": closes the program (the backup on exit still runs). */
    public void exit() {
        if (contentNavigator.confirmLeavingCurrent()) {
            closeOtherWindows();
            stage.close();
        }
    }

    public Window window() {
        return stage;
    }

    private void showEntryScreen() {
        boolean setup = authenticationService.isSetupRequired();
        Parent root = fxmlViewLoader.load(setup ? ViewPaths.SETUP_DIALOG : ViewPaths.LOGIN_DIALOG);
        stage.setMaximized(false);
        stage.setMinWidth(0);
        stage.setMinHeight(0);
        Scene scene = new Scene(root, LOGIN_WIDTH, setup ? SETUP_HEIGHT : LOGIN_HEIGHT);
        Stylesheets.apply(scene);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle(DialogUtil.message(setup ? "setup.title" : "login.windowTitle"));
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    private void showMain() {
        Parent root = fxmlViewLoader.load(ViewPaths.MAIN);
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, Math.min(MAIN_WIDTH, screen.getWidth()),
                Math.min(MAIN_HEIGHT, screen.getHeight()));
        Stylesheets.apply(scene);
        scene.getAccelerators().put(SWITCH_USER_KEYS, this::switchUser);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(MAIN_MIN_WIDTH);
        stage.setMinHeight(MAIN_MIN_HEIGHT);
        stage.setTitle(appSignature.windowTitle());
        stage.sizeToScene();
        stage.centerOnScreen();
        pendingJobsBadge.refresh();
    }

    /** Every dialog or alert still open (they belong to the previous user). */
    private void closeOtherWindows() {
        List<Window> open = new ArrayList<>(Window.getWindows());
        open.stream().filter(window -> window != stage).forEach(Window::hide);
    }
}
