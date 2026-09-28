package com.electrician.tracker.ui.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.service.PaymentService;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * A job's payment list ("Tahsilat" tab of a site, payments section of a
 * service), newest first: every payment received, editable (double click or
 * "Düzenle") and deletable. Only built for an ADMIN. Changes go through
 * {@link PaymentService}; the caller refreshes the job.
 */
public final class JobPaymentPane {

    private static final double ROW_HEIGHT = 28;
    private static final double HEADER_HEIGHT = 40;
    private static final double MAX_TABLE_HEIGHT = 200;
    private static final double SPACING = 10;
    private static final int DOUBLE_CLICK = 2;

    private final PaymentService paymentService;
    private final Consumer<Long> onChanged;
    private final Consumer<Payment> onEdit;

    /** {@code onChanged} receives the job id after a delete; {@code onEdit} opens the edit dialog. */
    public JobPaymentPane(PaymentService paymentService, Consumer<Long> onChanged, Consumer<Payment> onEdit) {
        this.paymentService = paymentService;
        this.onChanged = onChanged;
        this.onEdit = onEdit;
    }

    public VBox build(Long jobId, List<Payment> payments) {
        TableView<Payment> table = new TableView<>(TableSorting.sorted(payments, TableSorting.payments()));
        TableColumn<Payment, LocalDate> date = new TableColumn<>(DialogUtil.message("payment.field.date"));
        TableSorting.date(date, Payment::getPaymentDate);
        TableColumn<Payment, BigDecimal> amount = new TableColumn<>(DialogUtil.message("payment.field.amount"));
        TableSorting.money(amount, Payment::getAmount);
        TableColumn<Payment, String> method = new TableColumn<>(DialogUtil.message("payment.field.method"));
        TableSorting.text(method, p -> EnumLabels.label(p.getMethod()));
        TableColumn<Payment, String> note = new TableColumn<>(DialogUtil.message("payment.field.note"));
        TableSorting.text(note, Payment::getNote);
        table.getColumns().setAll(List.of(date, amount, method, note));
        table.setPlaceholder(new Label(DialogUtil.message("payment.empty")));
        table.setPrefHeight(Math.min(MAX_TABLE_HEIGHT, HEADER_HEIGHT + Math.max(payments.size(), 1) * ROW_HEIGHT));
        table.setRowFactory(tv -> {
            TableRow<Payment> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == DOUBLE_CLICK && !row.isEmpty()) {
                    onEdit.accept(row.getItem());
                }
            });
            return row;
        });

        Button editButton = new Button(DialogUtil.message("payment.action.editSelected"));
        editButton.setOnAction(e -> editSelected(table));
        Button deleteButton = new Button(DialogUtil.message("payment.action.deleteSelected"));
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setOnAction(e -> deleteSelected(table, jobId));
        HBox buttons = new HBox(SPACING, editButton, deleteButton);
        buttons.getStyleClass().add("table-actions");
        return new VBox(SPACING, table, buttons);
    }

    private void editSelected(TableView<Payment> table) {
        Payment selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        onEdit.accept(selected);
    }

    private void deleteSelected(TableView<Payment> table, Long jobId) {
        Payment selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        String description = DialogUtil.message("delete.single.payment", Bicimlendirici.date(selected.getPaymentDate()),
                Bicimlendirici.money(selected.getAmount()));
        if (!DeleteConfirmation.confirmSingle(description)) {
            return;
        }
        try {
            paymentService.delete(selected.getId());
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        onChanged.accept(jobId);
    }
}
