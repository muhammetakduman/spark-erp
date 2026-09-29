package com.electrician.tracker.ui.util;

import java.util.ArrayList;
import java.util.List;

import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.dto.JobDeletionImpact;

/**
 * The delete confirmation used everywhere something is deleted: it names what
 * will be deleted (for a job, how many records go with it), says it cannot be
 * undone, and starts on "Vazgeç" — Enter does nothing, only a click on the red
 * delete button deletes (see {@link SparkDialog}).
 */
public final class DeleteConfirmation {

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
        return SparkDialog.confirmDelete(question, details, deleteText);
    }
}
