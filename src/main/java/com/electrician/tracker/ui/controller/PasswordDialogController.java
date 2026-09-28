package com.electrician.tracker.ui.controller;

import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.UserService;
import com.electrician.tracker.ui.util.DialogUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Sets a new password (twice). Changing one's own password also asks for
 * the current one; an ADMIN resetting someone else's does not.
 */
@Component
@Scope("prototype")
public class PasswordDialogController {

    private final UserService userService;
    private final AccessControl accessControl;

    @FXML
    private Label userLabel;
    @FXML
    private Label currentPasswordLabel;
    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField repeatPasswordField;
    @FXML
    private Button saveButton;

    private Long userId;

    public PasswordDialogController(UserService userService, AccessControl accessControl) {
        this.userService = userService;
        this.accessControl = accessControl;
    }

    public void forUser(Long id, String fullName) {
        this.userId = id;
        userLabel.setText(fullName);
        boolean own = accessControl.currentUser().map(user -> user.id().equals(id)).orElse(false);
        currentPasswordLabel.setVisible(own);
        currentPasswordLabel.setManaged(own);
        currentPasswordField.setVisible(own);
        currentPasswordField.setManaged(own);
    }

    @FXML
    private void onSave() {
        try {
            userService.changePassword(userId, currentPasswordField.getText(), newPasswordField.getText(),
                    repeatPasswordField.getText());
            DialogUtil.showInfo("user.password.changed");
            close();
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }
}
