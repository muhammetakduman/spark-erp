package com.electrician.tracker.ui.controller;

import com.electrician.tracker.ui.util.AppSignature;
import com.electrician.tracker.ui.util.ContentNavigator;
import com.electrician.tracker.ui.util.ModalStageOpener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Component;

@Component
public class MainController {

    private static final String LICENSE_DIALOG_FXML = "/fxml/license_dialog.fxml";

    private final ContentNavigator contentNavigator;
    private final ModalStageOpener modalStageOpener;
    private final AppSignature appSignature;

    @FXML
    private StackPane contentArea;
    @FXML
    private Label signatureVersionLabel;
    @FXML
    private Label signatureNoticeLabel;
    @FXML
    private Label signatureCopyrightLabel;

    public MainController(ContentNavigator contentNavigator, ModalStageOpener modalStageOpener,
            AppSignature appSignature) {
        this.contentNavigator = contentNavigator;
        this.modalStageOpener = modalStageOpener;
        this.appSignature = appSignature;
    }

    @FXML
    private void initialize() {
        signatureVersionLabel.setText(appSignature.versionLine());
        signatureNoticeLabel.setText(appSignature.noticeLine());
        signatureCopyrightLabel.setText(appSignature.copyrightLine());
        contentNavigator.attach(contentArea);
        showHome();
    }

    @FXML
    private void showAbout() {
        modalStageOpener.<LicenseDialogController>openAndWait(LICENSE_DIALOG_FXML, "menu.about",
                contentArea.getScene().getWindow(), LicenseDialogController::showAsAbout);
    }

    @FXML
    private void showHome() {
        show("/fxml/home_screen.fxml");
    }

    @FXML
    private void showMonthlyReport() {
        show("/fxml/monthly_report.fxml");
    }

    @FXML
    private void showCustomers() {
        show("/fxml/customer_list.fxml");
    }

    @FXML
    private void showEmployees() {
        show("/fxml/employee_list.fxml");
    }

    @FXML
    private void showEmployeeAttendance() {
        show("/fxml/employee_attendance.fxml");
    }

    @FXML
    private void showProducts() {
        show("/fxml/product_list.fxml");
    }

    @FXML
    private void showSettings() {
        show("/fxml/settings_screen.fxml");
    }

    private void show(String fxmlPath) {
        contentNavigator.show(fxmlPath);
    }
}
