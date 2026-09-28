package com.electrician.tracker.ui.controller;

import java.util.List;

import com.electrician.tracker.domain.AppUser;
import com.electrician.tracker.domain.UserRole;
import com.electrician.tracker.dto.UserDraft;
import com.electrician.tracker.service.UserService;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EnumLabels;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Adds a user (username, name, title, role, password twice) or edits one
 * (name, title and role; the username stays, the password is changed with
 * its own dialog).
 */
@Component
@Scope("prototype")
public class UserFormController {

    private final UserService userService;

    @FXML
    private TextField usernameField;
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField titleField;
    @FXML
    private ComboBox<UserRole> roleComboBox;
    @FXML
    private Label passwordLabel;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label passwordRepeatLabel;
    @FXML
    private PasswordField passwordRepeatField;
    @FXML
    private Button saveButton;

    private Long editingId;
    private boolean saved;

    public UserFormController(UserService userService) {
        this.userService = userService;
    }

    @FXML
    private void initialize() {
        roleComboBox.getItems().setAll(UserRole.values());
        roleComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(UserRole role) {
                return EnumLabels.label(role);
            }

            @Override
            public UserRole fromString(String text) {
                return null;
            }
        });
        roleComboBox.setValue(UserRole.MANAGER);
    }

    public void editExisting(AppUser user) {
        this.editingId = user.getId();
        usernameField.setText(user.getUsername());
        usernameField.setDisable(true);
        fullNameField.setText(user.getFullName());
        titleField.setText(user.getTitle());
        roleComboBox.setValue(user.getRole());
        for (Node node : List.of(passwordLabel, passwordField, passwordRepeatLabel, passwordRepeatField)) {
            node.setVisible(false);
            node.setManaged(false);
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onSave() {
        try {
            if (editingId == null) {
                userService.create(new UserDraft(usernameField.getText(), fullNameField.getText(), titleField.getText(),
                        roleComboBox.getValue(), passwordField.getText(), passwordRepeatField.getText()));
            } else {
                userService.update(editingId, fullNameField.getText(), titleField.getText(), roleComboBox.getValue());
            }
            saved = true;
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
