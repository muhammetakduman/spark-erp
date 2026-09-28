package com.electrician.tracker.ui.controller;

import java.util.List;
import java.util.Optional;

import com.electrician.tracker.domain.AppUser;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.UserService;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.TableSorting;
import com.electrician.tracker.ui.util.ViewPaths;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Kullanıcılar" (ADMIN only): list, add, edit and deactivate users and reset
 * passwords. Everybody changes their own password from the user menu. The
 * service refuses to deactivate or demote the last active ADMIN.
 */
@Component
@Scope("prototype")
public class UserSettingsController {

    private final UserService userService;
    private final AccessControl accessControl;
    private final ModalStageOpener modalStageOpener;

    @FXML
    private TableView<AppUser> userTable;
    @FXML
    private TableColumn<AppUser, String> usernameColumn;
    @FXML
    private TableColumn<AppUser, String> fullNameColumn;
    @FXML
    private TableColumn<AppUser, String> titleColumn;
    @FXML
    private TableColumn<AppUser, String> roleColumn;
    @FXML
    private TableColumn<AppUser, String> activeColumn;

    public UserSettingsController(UserService userService, AccessControl accessControl,
            ModalStageOpener modalStageOpener) {
        this.userService = userService;
        this.accessControl = accessControl;
        this.modalStageOpener = modalStageOpener;
    }

    @FXML
    private void initialize() {
        accessControl.requireAdmin();
        TableSorting.text(usernameColumn, AppUser::getUsername);
        TableSorting.text(fullNameColumn, AppUser::getFullName);
        TableSorting.text(titleColumn, AppUser::getTitle);
        TableSorting.text(roleColumn, user -> EnumLabels.label(user.getRole()));
        TableSorting.text(activeColumn, user -> DialogUtil.message(user.isActive() ? "common.yes" : "common.no"));
        refresh();
    }

    private void refresh() {
        List<AppUser> users = userService.findAll();
        userTable.getItems().setAll(users);
    }

    @FXML
    private void onNewUser() {
        UserFormController form = modalStageOpener.openAndWait(ViewPaths.USER_FORM, "user.dialog.new", window());
        if (form.isSaved()) {
            refresh();
        }
    }

    @FXML
    private void onEditUser() {
        selected().ifPresent(user -> {
            UserFormController form = modalStageOpener.openAndWait(ViewPaths.USER_FORM, "user.dialog.edit", window(),
                    controller -> controller.editExisting(user));
            if (form.isSaved()) {
                refresh();
            }
        });
    }

    @FXML
    private void onResetPassword() {
        selected().ifPresent(user -> openPasswordDialog(user.getId(), user.getFullName()));
    }

    private void openPasswordDialog(Long userId, String fullName) {
        modalStageOpener.<PasswordDialogController>openAndWait(ViewPaths.PASSWORD_DIALOG, "user.dialog.password",
                window(), controller -> controller.forUser(userId, fullName));
    }

    @FXML
    private void onToggleActive() {
        selected().ifPresent(user -> {
            try {
                userService.setActive(user.getId(), !user.isActive());
            } catch (RuntimeException ex) {
                DialogUtil.showError(ex);
            }
            refresh();
        });
    }

    private Optional<AppUser> selected() {
        AppUser user = userTable.getSelectionModel().getSelectedItem();
        if (user == null) {
            DialogUtil.showErrorMessage("error.selection.required");
        }
        return Optional.ofNullable(user);
    }

    private Window window() {
        return userTable.getScene().getWindow();
    }
}
