package com.electrician.tracker.ui.controller;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;

import com.electrician.tracker.service.BackupService;
import com.electrician.tracker.service.ReportService;
import com.electrician.tracker.ui.util.DialogUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

@Component
public class SettingsController {

    private final BackupService backupService;
    private final ReportService reportService;

    @FXML
    private Label backupFolderLabel;
    @FXML
    private DatePicker exportStartDatePicker;
    @FXML
    private DatePicker exportEndDatePicker;

    public SettingsController(BackupService backupService, ReportService reportService) {
        this.backupService = backupService;
        this.reportService = reportService;
    }

    @FXML
    private void initialize() {
        backupFolderLabel.setText(backupService.getBackupFolder().toString());
        exportStartDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        exportEndDatePicker.setValue(LocalDate.now());
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
