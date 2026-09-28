package com.electrician.tracker.ui.controller;

import java.util.List;

import com.electrician.tracker.dto.SessionUser;
import com.electrician.tracker.service.AuthenticationService;
import com.electrician.tracker.ui.util.AppIcon;
import com.electrician.tracker.ui.util.AppSignature;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.IconSize;
import com.electrician.tracker.ui.util.Icons;
import com.electrician.tracker.ui.util.StageManager;
import com.electrician.tracker.ui.util.WindowDecorations;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Login: brand band on the left, form on the right. Enter logs in from both
 * fields; the eye shows the password while it is held down. A wrong login
 * marks the fields red with the reason below them (no pop-up); while the
 * password is checked the button shows a spinner and cannot be pressed
 * twice. After five wrong passwords logins pause for 30 seconds.
 */
@Component
@Scope("prototype")
public class LoginController {

    private static final double SPINNER_SIZE = 16;
    private static final String ERROR_STYLE = "icon-field-error";

    private final AuthenticationService authenticationService;
    private final StageManager stageManager;
    private final AppSignature appSignature;

    @FXML
    private ImageView appIconView;
    @FXML
    private Label versionLabel;
    @FXML
    private Label developerLabel;
    @FXML
    private HBox usernameBox;
    @FXML
    private HBox passwordBox;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField visiblePasswordField;
    @FXML
    private Button showPasswordButton;
    @FXML
    private Label errorLabel;
    @FXML
    private Button loginButton;

    public LoginController(AuthenticationService authenticationService, StageManager stageManager,
            AppSignature appSignature) {
        this.authenticationService = authenticationService;
        this.stageManager = stageManager;
        this.appSignature = appSignature;
    }

    @FXML
    private void initialize() {
        appIconView.setImage(WindowDecorations.largeIcon());
        versionLabel.setText(appSignature.versionOnly());
        developerLabel.setText(appSignature.developer());
        usernameBox.getChildren().add(0, Icons.of(AppIcon.USER, IconSize.BUTTON));
        passwordBox.getChildren().add(0, Icons.of(AppIcon.PASSWORD_FIELD, IconSize.BUTTON));
        setUpPasswordPeek();
        usernameField.textProperty().addListener((obs, o, n) -> clearError());
        passwordField.textProperty().addListener((obs, o, n) -> clearError());
        Platform.runLater(usernameField::requestFocus);
    }

    /** The eye shows the password only while it is held down. */
    private void setUpPasswordPeek() {
        showPasswordButton.setGraphic(Icons.of(AppIcon.SHOW_PASSWORD, IconSize.BUTTON));
        visiblePasswordField.textProperty().bind(passwordField.textProperty());
        showPasswordButton.setOnMousePressed(event -> peek(true));
        showPasswordButton.setOnMouseReleased(event -> peek(false));
        showPasswordButton.setOnMouseExited(event -> peek(false));
    }

    private void peek(boolean visible) {
        visiblePasswordField.setVisible(visible);
        passwordField.setVisible(!visible);
    }

    @FXML
    private void onLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        Task<SessionUser> task = new Task<>() {
            @Override
            protected SessionUser call() {
                return authenticationService.login(username, password);
            }
        };
        setBusy(true);
        task.setOnSucceeded(event -> stageManager.onLoggedIn());
        task.setOnFailed(event -> {
            setBusy(false);
            showError(task.getException());
        });
        Thread thread = new Thread(task, "login");
        thread.setDaemon(true);
        thread.start();
    }

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
        usernameField.setDisable(busy);
        passwordField.setDisable(busy);
        if (busy) {
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setPrefSize(SPINNER_SIZE, SPINNER_SIZE);
            spinner.getStyleClass().add("button-spinner");
            loginButton.setGraphic(spinner);
        } else {
            loginButton.setGraphic(null);
        }
    }

    /** The password is cleared first: typing (or clearing) removes the error, so it must come after. */
    private void showError(Throwable failure) {
        passwordField.clear();
        String text = failure instanceof RuntimeException runtime ? DialogUtil.errorText(runtime)
                : DialogUtil.message("error.unexpected", String.valueOf(failure.getMessage()));
        errorLabel.setText(text);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        fieldBoxes().forEach(box -> {
            if (!box.getStyleClass().contains(ERROR_STYLE)) {
                box.getStyleClass().add(ERROR_STYLE);
            }
        });
        passwordField.requestFocus();
    }

    private void clearError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        fieldBoxes().forEach(box -> box.getStyleClass().remove(ERROR_STYLE));
    }

    private List<Node> fieldBoxes() {
        return List.of(usernameBox, passwordBox);
    }

    @FXML
    private void onExit() {
        Window window = loginButton.getScene().getWindow();
        window.hide();
    }
}
