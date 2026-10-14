package com.electrician.tracker.ui.controller;

import java.util.Objects;
import java.util.Optional;

import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.PhoneNumbers;
import com.electrician.tracker.service.messaging.SmsGateway;
import com.electrician.tracker.service.messaging.SmsResult;
import com.electrician.tracker.service.messaging.SmsSettings;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.TaskRunner;
import com.electrician.tracker.ui.util.UnsavedChangesAware;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Mesajlaşma" in Settings (ADMIN only): the SMS provider account this
 * installation sends through.
 *
 * <p>WhatsApp needs nothing configured — it opens the user's own application
 * with the text ready — so the section explains that and then asks only for
 * what SMS requires. The account belongs to the user: they buy the credit,
 * they own the sender title, and the test button proves the whole chain
 * against their real account before anyone relies on it.
 */
@Component
@Scope("prototype")
public class MessagingSettingsController implements UnsavedChangesAware {

    private final SmsSettings smsSettings;
    private final SmsGateway smsGateway;
    private final AccessControl accessControl;
    private final TaskRunner taskRunner;

    @FXML
    private VBox messagingSection;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField headerField;
    @FXML
    private TextField baseUrlField;
    @FXML
    private TextField testNumberField;
    @FXML
    private Button testButton;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private Label stateLabel;

    private String shownUsername;
    private String shownPassword;
    private String shownHeader;
    private String shownBaseUrl;

    public MessagingSettingsController(SmsSettings smsSettings, SmsGateway smsGateway, AccessControl accessControl,
            TaskRunner taskRunner) {
        this.smsSettings = smsSettings;
        this.smsGateway = smsGateway;
        this.accessControl = accessControl;
        this.taskRunner = taskRunner;
    }

    @FXML
    private void initialize() {
        boolean admin = accessControl.isAdmin();
        messagingSection.setVisible(admin);
        messagingSection.setManaged(admin);
        if (admin) {
            refresh();
        }
    }

    @Override
    public boolean hasUnsavedChanges() {
        return !Objects.equals(shownUsername, usernameField.getText())
                || !Objects.equals(shownPassword, passwordField.getText())
                || !Objects.equals(shownHeader, headerField.getText())
                || !Objects.equals(shownBaseUrl, baseUrlField.getText());
    }

    private void refresh() {
        shownUsername = smsSettings.username();
        shownPassword = smsSettings.password();
        shownHeader = smsSettings.header();
        shownBaseUrl = smsSettings.baseUrl();
        usernameField.setText(shownUsername);
        passwordField.setText(shownPassword);
        headerField.setText(shownHeader);
        baseUrlField.setText(shownBaseUrl);
        showState();
    }

    private void showState() {
        stateLabel.setText(DialogUtil.message(
                smsSettings.isConfigured() ? "settings.messaging.state.ready" : "settings.messaging.state.missing"));
        testButton.setDisable(!smsSettings.isConfigured());
    }

    @FXML
    private void onSave() {
        try {
            smsSettings.save(baseUrlField.getText(), usernameField.getText(), passwordField.getText(),
                    headerField.getText());
            refresh();
            DialogUtil.showInfo("settings.messaging.saved");
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    /**
     * Sends one real message, because nothing short of that proves the
     * credentials, the sender title and the endpoint all line up. It costs the
     * user one SMS, so it is asked for rather than done on save.
     */
    @FXML
    private void onTest() {
        if (hasUnsavedChanges()) {
            DialogUtil.showErrorMessage("settings.messaging.saveFirst");
            return;
        }
        Optional<String> phone = PhoneNumbers.toWhatsApp(testNumberField.getText());
        if (phone.isEmpty()) {
            DialogUtil.showErrorMessage("message.phone.unreadable");
            return;
        }
        if (!DialogUtil.confirm("settings.messaging.test.confirm", phone.get())) {
            return;
        }
        String text = DialogUtil.message("settings.messaging.test.body");
        taskRunner.run(() -> smsGateway.send(phone.get(), text), this::showTestResult, loadingIndicator,
                messagingSection);
    }

    private void showTestResult(SmsResult result) {
        if (result.success()) {
            DialogUtil.showInfo("settings.messaging.test.sent");
            return;
        }
        String detail = result.detail() == null || result.detail().isBlank()
                ? DialogUtil.message(result.code())
                : DialogUtil.message(result.code()) + " (" + result.detail() + ")";
        DialogUtil.showInfoText(detail);
    }
}
