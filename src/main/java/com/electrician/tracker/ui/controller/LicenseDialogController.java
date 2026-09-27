package com.electrician.tracker.ui.controller;

import com.electrician.tracker.service.LicenseService;
import com.electrician.tracker.ui.util.AppSignature;
import com.electrician.tracker.ui.util.DialogUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Shows the licence with the developer's signature. On first run (and after
 * an update) the user must tick "Okudum, kabul ediyorum" before continuing;
 * from the menu it opens read-only as the About dialog.
 */
@Component
@Scope("prototype")
public class LicenseDialogController {

    private final LicenseService licenseService;
    private final AppSignature appSignature;

    @FXML
    private Label versionLabel;
    @FXML
    private Label noticeLabel;
    @FXML
    private TextArea licenseTextArea;
    @FXML
    private CheckBox acceptCheckBox;
    @FXML
    private Label copyrightLabel;
    @FXML
    private Button continueButton;
    @FXML
    private Button exitButton;

    private boolean accepted;

    public LicenseDialogController(LicenseService licenseService, AppSignature appSignature) {
        this.licenseService = licenseService;
        this.appSignature = appSignature;
    }

    @FXML
    private void initialize() {
        versionLabel.setText(appSignature.versionLine());
        noticeLabel.setText(appSignature.noticeLine());
        copyrightLabel.setText(appSignature.copyrightLine());
        licenseTextArea.setText(licenseService.licenseText());
        continueButton.disableProperty().bind(acceptCheckBox.selectedProperty().not());
    }

    /** Read-only "Hakkında" view: no acceptance, a single close button. */
    public void showAsAbout() {
        acceptCheckBox.setVisible(false);
        acceptCheckBox.setManaged(false);
        continueButton.setVisible(false);
        continueButton.setManaged(false);
        exitButton.setText(DialogUtil.message("action.close"));
    }

    public boolean isAccepted() {
        return accepted;
    }

    @FXML
    private void onContinue() {
        licenseService.accept();
        accepted = true;
        close();
    }

    @FXML
    private void onExit() {
        close();
    }

    private void close() {
        ((Stage) licenseTextArea.getScene().getWindow()).close();
    }
}
