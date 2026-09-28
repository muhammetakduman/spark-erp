package com.electrician.tracker.ui.util;

import java.util.ArrayList;
import java.util.List;

import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.dto.JobDeletionImpact;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/**
 * The delete confirmation used everywhere something is deleted: it names what
 * will be deleted (for a job, how many records go with it), says it cannot be
 * undone, and starts on "Vazgeç" — Enter cancels, only a click on the red
 * delete button deletes.
 */
public final class DeleteConfirmation {

    private static final String DANGER_STYLE = "danger-button";
    private static final String BULLET = "· ";

    private DeleteConfirmation() {
    }

    /** "…şantiyesini silmek istediğinize emin misiniz?" with the material/attendance/payment counts. */
    public static boolean confirmJob(JobDeletionImpact impact) {
        boolean site = impact.jobType() == JobType.SITE;
        String question = DialogUtil.message(site ? "delete.site.question" : "delete.service.question",
                impact.jobLabel());
        String buttonKey = site ? "delete.site.button" : "delete.service.button";
        return ask(question, impactText(impact), DialogUtil.message(buttonKey));
    }

    /** A single record (a material line, an attendance row, …): one descriptive line is enough. */
    public static boolean confirmSingle(String description) {
        return ask(DialogUtil.message("delete.single.question"), description + "\n\n"
                + DialogUtil.message("delete.irreversible"), DialogUtil.message("delete.single.button"));
    }

    private static String impactText(JobDeletionImpact impact) {
        List<String> lines = new ArrayList<>();
        addCount(lines, impact.materialCount(), "delete.count.materials");
        addCount(lines, impact.attendanceCount(), "delete.count.attendance");
        addCount(lines, impact.paymentCount(), "delete.count.payments");
        String details = lines.isEmpty() ? DialogUtil.message("delete.count.none")
                : DialogUtil.message("delete.count.title") + "\n" + String.join("\n", lines);
        return details + "\n\n" + DialogUtil.message("delete.irreversible");
    }

    private static void addCount(List<String> lines, long count, String messageKey) {
        if (count > 0) {
            lines.add(BULLET + DialogUtil.message(messageKey, String.valueOf(count)));
        }
    }

    private static boolean ask(String question, String details, String deleteText) {
        ButtonType cancel = new ButtonType(DialogUtil.message("delete.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType delete = new ButtonType(deleteText, ButtonBar.ButtonData.OTHER);
        Alert alert = new Alert(Alert.AlertType.WARNING, details, cancel, delete);
        alert.setTitle(DialogUtil.message("delete.title"));
        alert.setHeaderText(question);
        Stylesheets.apply(alert.getDialogPane());
        Button cancelButton = (Button) alert.getDialogPane().lookupButton(cancel);
        Button deleteButton = (Button) alert.getDialogPane().lookupButton(delete);
        cancelButton.setDefaultButton(true);
        deleteButton.setDefaultButton(false);
        deleteButton.getStyleClass().add(DANGER_STYLE);
        Platform.runLater(cancelButton::requestFocus);
        return alert.showAndWait().filter(delete::equals).isPresent();
    }
}
