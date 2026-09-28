package com.electrician.tracker.ui.controller;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.BackupService;
import com.electrician.tracker.service.ReportService;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.UnsavedChangesAware;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Ayarlar": company details, exchange rates, backup/restore and the
 * jobs-by-date Excel export (ADMIN only), and templates (everybody). Company,
 * rates and templates are their own included views; leaving the screen with
 * unsaved company or rate changes asks first.
 */
@Component
@Scope("prototype")
public class SettingsController implements UnsavedChangesAware {

    private final BackupService backupService;
    private final ReportService reportService;
    private final AccessControl accessControl;

    @FXML
    private Node backupSection;
    @FXML
    private Node exportSection;
    @FXML
    private CompanySettingsController companyController;
    @FXML
    private ExchangeRateSettingsController ratesController;
    @FXML
    private Label backupFolderLabel;
    @FXML
    private DatePicker exportStartDatePicker;
    @FXML
    private DatePicker exportEndDatePicker;

    public SettingsController(BackupService backupService, ReportService reportService,
            AccessControl accessControl) {
        this.backupService = backupService;
        this.reportService = reportService;
        this.accessControl = accessControl;
    }

    @FXML
    private void initialize() {
        boolean admin = accessControl.isAdmin();
        for (Node section : List.of(backupSection, exportSection)) {
            section.setVisible(admin);
            section.setManaged(admin);
        }
        backupFolderLabel.setText(backupService.getBackupFolder().toString());
        exportStartDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        exportEndDatePicker.setValue(LocalDate.now());
    }

    @Override
    public boolean hasUnsavedChanges() {
        return companyController.hasUnsavedChanges() || ratesController.hasUnsavedChanges();
    }

    @FXML
    private void onChooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(DialogUtil.message("settings.chooseFolder.title"));
        File selected = chooser.showDialog(window());
        if (selected != null) {
            backupService.setBackupFolder(selected.toPath());
            backupFolderLabel.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    private void onBackupNow() {
        Path backupFile = backupService.backupNow();
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                DialogUtil.message("settings.backupNow.success") + " " + backupFile);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML
    private void onRestore() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(DialogUtil.message("settings.restore.chooseFile"));
        chooser.setInitialDirectory(backupService.getBackupFolder().toFile());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite (*.db)", "*.db"));
        File selected = chooser.showOpenDialog(window());
        if (selected == null) {
            return;
        }
        if (!DialogUtil.confirm("settings.restore.confirm")) {
            return;
        }
        backupService.restoreFromBackup(selected.toPath());
        Alert alert = new Alert(Alert.AlertType.INFORMATION, DialogUtil.message("settings.restore.done"));
        alert.setHeaderText(null);
        alert.showAndWait();
        Platform.exit();
    }

    @FXML
    private void onExportExcel() {
        LocalDate start = exportStartDatePicker.getValue();
        LocalDate end = exportEndDatePicker.getValue();
        if (start == null || end == null || start.isAfter(end)) {
            DialogUtil.showErrorMessage("error.export.dateRange.invalid");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(DialogUtil.message("settings.exportExcel.title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        chooser.setInitialFileName("isler_" + start + "_" + end + ".xlsx");
        File selected = chooser.showSaveDialog(window());
        if (selected == null) {
            return;
        }
        reportService.exportJobsByDateRange(start, end, selected.toPath());
        Alert alert = new Alert(Alert.AlertType.INFORMATION, DialogUtil.message("settings.exportExcel.success"));
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private Window window() {
        return backupFolderLabel.getScene().getWindow();
    }
}
