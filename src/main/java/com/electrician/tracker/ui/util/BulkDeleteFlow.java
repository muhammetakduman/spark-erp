package com.electrician.tracker.ui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.electrician.tracker.dto.BulkDeletionResult;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * "Seçilenleri Sil" from start to end: counts first ("12 kaydı silmek
 * üzeresiniz" with what goes with them and the first five names), confirms
 * (typing the number above ten records), deletes in the background (with a
 * progress window above {@value #PROGRESS_THRESHOLD} records), then reports.
 * Records that could not be deleted are listed with their reason and, where
 * the service allows it, can be made inactive with one click.
 */
public final class BulkDeleteFlow<T> {

    static final int PROGRESS_THRESHOLD = 500;
    private static final int NAMES_SHOWN = 5;
    private static final String BULLET = "· ";
    private static final String NEW_LINE = "\n";
    private static final double PROGRESS_SPACING = 10;

    private final List<T> items;
    private final Function<T, Long> idOf;
    private final Function<T, String> labelOf;
    private String itemCountKey = "bulk.count.records";
    private Function<List<Long>, Map<String, Long>> impact = ids -> Map.of();
    private Function<List<Long>, BulkDeletionResult> delete;
    private Consumer<List<Long>> deactivate;
    private Runnable afterwards = () -> {
    };

    private BulkDeleteFlow(List<T> items, Function<T, Long> idOf, Function<T, String> labelOf) {
        this.items = List.copyOf(items);
        this.idOf = idOf;
        this.labelOf = labelOf;
    }

    public static <T> BulkDeleteFlow<T> of(List<T> items, Function<T, Long> idOf, Function<T, String> labelOf) {
        return new BulkDeleteFlow<>(items, idOf, labelOf);
    }

    /** "{0} şantiye" — the first line of "Bu işlemle birlikte silinecekler". */
    public BulkDeleteFlow<T> itemCount(String messageKey) {
        this.itemCountKey = messageKey;
        return this;
    }

    /** Related records that are deleted too (message key → count). */
    public BulkDeleteFlow<T> impact(Function<List<Long>, Map<String, Long>> value) {
        this.impact = value;
        return this;
    }

    public BulkDeleteFlow<T> delete(Function<List<Long>, BulkDeletionResult> value) {
        this.delete = value;
        return this;
    }

    /** Offers "Seçilenleri pasife al" for the records that could not be deleted. */
    public BulkDeleteFlow<T> deactivate(Consumer<List<Long>> value) {
        this.deactivate = value;
        return this;
    }

    /** Runs after anything was changed (e.g. reloads the list). */
    public BulkDeleteFlow<T> afterwards(Runnable value) {
        this.afterwards = value;
        return this;
    }

    public void run() {
        if (items.isEmpty()) {
            return;
        }
        List<Long> ids = items.stream().map(idOf).toList();
        Map<String, Long> related;
        try {
            related = impact.apply(ids);
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
            return;
        }
        if (!SparkDialog.confirmDelete(DialogUtil.message("bulk.confirm.title", items.size()),
                confirmationText(related), items.size())) {
            return;
        }
        deleteInBackground(ids);
    }

    /** "Bu işlemle birlikte silinecekler: · 12 şantiye · 148 malzeme kalemi …", the names, "geri alınamaz". */
    private String confirmationText(Map<String, Long> related) {
        List<String> lines = new ArrayList<>();
        lines.add(DialogUtil.message("delete.count.title"));
        lines.add(BULLET + DialogUtil.message(itemCountKey, items.size()));
        related.forEach((key, count) -> lines.add(BULLET + DialogUtil.message(key, String.valueOf(count))));
        lines.add("");
        lines.add(DialogUtil.message("bulk.confirm.names"));
        items.stream().limit(NAMES_SHOWN).forEach(item -> lines.add(BULLET + labelOf.apply(item)));
        if (items.size() > NAMES_SHOWN) {
            lines.add(DialogUtil.message("bulk.confirm.more", items.size() - NAMES_SHOWN));
        }
        lines.add("");
        lines.add(DialogUtil.message("delete.irreversible"));
        return String.join(NEW_LINE, lines);
    }

    private void deleteInBackground(List<Long> ids) {
        Stage progress = ids.size() > PROGRESS_THRESHOLD ? progressWindow() : null;
        Task<BulkDeletionResult> task = new Task<>() {
            @Override
            protected BulkDeletionResult call() {
                return delete.apply(ids);
            }
        };
        task.setOnSucceeded(event -> {
            closeProgress(progress);
            afterwards.run();
            report(task.getValue());
        });
        task.setOnFailed(event -> {
            closeProgress(progress);
            afterwards.run();
            Throwable failure = task.getException();
            if (failure instanceof RuntimeException runtime) {
                DialogUtil.showError(runtime);
            } else {
                DialogUtil.showUnexpected(failure);
            }
        });
        Thread thread = new Thread(task, "bulk-delete");
        thread.setDaemon(true);
        thread.start();
    }

    /** "12 kayıt silindi." or the list of records that stayed, with "Seçilenleri pasife al". */
    private void report(BulkDeletionResult result) {
        if (result.skipped().isEmpty()) {
            SparkDialog.success(DialogUtil.message("bulk.deleted", result.deletedCount()));
            return;
        }
        VBox list = new VBox();
        list.getStyleClass().add("bulk-result-list");
        result.skipped().forEach(skipped -> list.getChildren().add(skippedLine(skipped)));
        SparkDialog.Builder dialog = SparkDialog.builder(SparkDialog.Kind.WARNING)
                .title(DialogUtil.message("bulk.partial.title", result.deletedCount(), result.skipped().size()))
                .detail(DialogUtil.message("bulk.partial.detail"))
                .content(list);
        if (deactivate == null) {
            dialog.build().showAndWait();
            return;
        }
        boolean confirmed = dialog.primary(DialogUtil.message("bulk.deactivate"))
                .secondary(DialogUtil.message("action.close")).build().showAndWait();
        if (confirmed) {
            deactivateSkipped(result.skippedIds());
        }
    }

    private Label skippedLine(BulkDeletionResult.Skipped skipped) {
        String name = items.stream().filter(item -> idOf.apply(item).equals(skipped.id())).findFirst()
                .map(labelOf).orElse(String.valueOf(skipped.id()));
        Label line = new Label(BULLET + name + " — " + DialogUtil.message(skipped.reasonKey(),
                String.valueOf(skipped.count())));
        line.setWrapText(true);
        return line;
    }

    private void deactivateSkipped(List<Long> ids) {
        try {
            deactivate.accept(ids);
            afterwards.run();
            SparkDialog.success(DialogUtil.message("bulk.deactivated", ids.size()));
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    /** "Kayıtlar siliniyor…" with a spinner, so a long delete never looks frozen. */
    private static Stage progressWindow() {
        ProgressIndicator indicator = new ProgressIndicator();
        Label text = new Label(DialogUtil.message("bulk.progress"));
        VBox box = new VBox(PROGRESS_SPACING, indicator, text);
        box.getStyleClass().addAll("spark-dialog", "spark-dialog-info", "bulk-progress");
        Stage stage = new Stage(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        Scene scene = new Scene(box);
        Stylesheets.apply(scene);
        stage.setScene(scene);
        stage.show();
        stage.centerOnScreen();
        return stage;
    }

    private static void closeProgress(Stage progress) {
        if (progress != null) {
            progress.close();
        }
    }
}
