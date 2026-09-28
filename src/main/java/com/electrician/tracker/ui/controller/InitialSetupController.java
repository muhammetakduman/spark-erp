package com.electrician.tracker.ui.controller;

import com.electrician.tracker.service.AuthenticationService;
import com.electrician.tracker.ui.util.AppSignature;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.StageManager;
import com.electrician.tracker.ui.util.WindowDecorations;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * First start ("İlk kurulum"), in the same design as the login: no user
 * exists yet, so the owner creates the administrator account (the password
 * is asked twice). That user is logged in right away. Mistakes are shown
 * under the form, not in a pop-up.
 */
@Component
@Scope("prototype")
public class InitialSetupController {

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
    private TextField usernameField;
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField titleField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField passwordRepeatField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button saveButton;

    public InitialSetupController(AuthenticationService authenticationService, StageManager stageManager,
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
        Platform.runLater(usernameField::requestFocus);
    }

    @FXML
    private void onSave() {
        try {
            authenticationService.setUpFirstAdmin(usernameField.getText(), fullNameField.getText(),
                    titleField.getText(), passwordField.getText(), passwordRepeatField.getText());
            stageManager.onLoggedIn();
        } catch (RuntimeException ex) {
            errorLabel.setText(DialogUtil.errorText(ex));
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    @FXML
    private void onExit() {
        saveButton.getScene().getWindow().hide();
    }
}
