package com.electrician.tracker.ui.util;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.service.PaymentService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * A job's payment list ("Tahsilat" tab of a site, payments section of a
 * service): every payment received, editable (double click or "Düzenle") and
 * deletable. Changes go through {@link PaymentService}; the
 * caller refreshes the job.
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
        TableView<Payment> table = new TableView<>(FXCollections.observableArrayList(payments));
        table.getColumns().setAll(List.of(
                column("payment.field.date", p -> Bicimlendirici.date(p.getPaymentDate())),
                column("payment.field.amount", p -> Bicimlendirici.money(p.getAmount())),
                column("payment.field.method", p -> EnumLabels.label(p.getMethod())),
                column("payment.field.note", p -> p.getNote() == null ? "" : p.getNote())));
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
        if (!DialogUtil.confirm("payment.confirm.delete")) {
            return;
        }
        try {
            paymentService.delete(selected.getId());
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        onChanged.accept(jobId);
    }

    private static TableColumn<Payment, String> column(String titleKey, Function<Payment, String> value) {
        TableColumn<Payment, String> column = new TableColumn<>(DialogUtil.message(titleKey));
        column.setCellValueFactory(d -> new SimpleStringProperty(value.apply(d.getValue())));
        return column;
    }
}
