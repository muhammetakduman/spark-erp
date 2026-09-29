package com.electrician.tracker.ui.controller;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.electrician.tracker.domain.QuoteStatus;
import com.electrician.tracker.dto.QuoteRow;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.QuoteService;
import com.electrician.tracker.ui.util.AppIcon;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.BulkDeleteFlow;
import com.electrician.tracker.ui.util.BulkSelection;
import com.electrician.tracker.ui.util.DeleteConfirmation;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EmptyState;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.TableSorting;
import com.electrician.tracker.ui.util.TaskRunner;
import com.electrician.tracker.ui.util.ViewPaths;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Teklifler": every quote, newest first, with a status filter. A quote that
 * was sent, is past its validity and still unanswered carries a yellow
 * warning badge. Double click opens it; "Kopyala" makes a new draft with the
 * next number. Only an ADMIN deletes quotes.
 */
@Component
@Scope("prototype")
public class QuoteListController {

    private static final int DOUBLE_CLICK = 2;
    private static final double BADGE_SPACING = 6;
    private static final String ALL = "ALL";

    private final QuoteService quoteService;
    private final AccessControl accessControl;
    private final ModalStageOpener modalStageOpener;
    private final TaskRunner taskRunner;

    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private ToggleGroup statusGroup;
    @FXML
    private Button deleteButton;
    @FXML
    private TableView<QuoteRow> quoteTable;
    @FXML
    private TableColumn<QuoteRow, String> numberColumn;
    @FXML
    private TableColumn<QuoteRow, LocalDate> dateColumn;
    @FXML
    private TableColumn<QuoteRow, String> companyColumn;
    @FXML
    private TableColumn<QuoteRow, String> subjectColumn;
    @FXML
    private TableColumn<QuoteRow, BigDecimal> totalColumn;
    @FXML
    private TableColumn<QuoteRow, String> statusColumn;
    @FXML
    private TableColumn<QuoteRow, QuoteRow> validityColumn;

    private FilteredList<QuoteRow> filteredQuotes;

    public QuoteListController(QuoteService quoteService, AccessControl accessControl,
            ModalStageOpener modalStageOpener, TaskRunner taskRunner) {
        this.quoteService = quoteService;
        this.accessControl = accessControl;
        this.modalStageOpener = modalStageOpener;
        this.taskRunner = taskRunner;
    }

    @FXML
    private void initialize() {
        quoteTable.setPlaceholder(EmptyState.of(AppIcon.QUOTES, "quote.list.empty",
                "quote.action.new", this::onNew));
        deleteButton.setVisible(accessControl.isAdmin());
        deleteButton.setManaged(accessControl.isAdmin());
        TableSorting.text(numberColumn, QuoteRow::quoteNo);
        TableSorting.date(dateColumn, QuoteRow::quoteDate);
        TableSorting.text(companyColumn, QuoteRow::companyName);
        TableSorting.text(subjectColumn, QuoteRow::subject);
        TableSorting.money(totalColumn, QuoteRow::grandTotal);
        TableSorting.text(statusColumn, row -> EnumLabels.label(row.status()));
        validityColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        validityColumn.setCellFactory(column -> new ValidityCell());
        validityColumn.setComparator(java.util.Comparator.comparing(QuoteRow::validUntil));
        if (accessControl.isAdmin()) {
            BulkSelection.forTable(quoteTable, QuoteRow::id, this::deleteSelected);
        }
        quoteTable.setRowFactory(table -> {
            TableRow<QuoteRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == DOUBLE_CLICK && !row.isEmpty()) {
                    open(row.getItem().id());
                }
            });
            return row;
        });
        statusGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                statusGroup.selectToggle(oldToggle);
                return;
            }
            applyFilter();
        });
        refresh();
    }

    private void refresh() {
        taskRunner.run(quoteService::findAllRows, this::show, loadingIndicator, quoteTable);
    }

    private void show(List<QuoteRow> rows) {
        ObservableList<QuoteRow> sorted = TableSorting.sorted(rows, TableSorting.quotes());
        filteredQuotes = new FilteredList<>(sorted);
        TableSorting.bindSorted(quoteTable, filteredQuotes);
        applyFilter();
    }

    private void applyFilter() {
        if (filteredQuotes == null) {
            return;
        }
        String status = (String) statusGroup.getSelectedToggle().getUserData();
        filteredQuotes.setPredicate(row -> ALL.equals(status) || row.status() == QuoteStatus.valueOf(status));
    }

    @FXML
    private void onNew() {
        QuoteFormController form = modalStageOpener.openAndWait(ViewPaths.QUOTE_FORM, "quote.dialog.new", window(),
                QuoteFormController::startNew);
        if (form.isChanged()) {
            refresh();
        }
    }

    @FXML
    private void onOpen() {
        selected().ifPresent(row -> open(row.id()));
    }

    private void open(Long quoteId) {
        QuoteFormController form = modalStageOpener.openAndWait(ViewPaths.QUOTE_FORM, "quote.dialog.edit", window(),
                controller -> controller.editExisting(quoteId));
        if (form.isChanged()) {
            refresh();
        }
    }

    /** A new draft with the same lines and the next number, opened right away. */
    @FXML
    private void onCopy() {
        selected().ifPresent(row -> {
            try {
                Long copyId = quoteService.copy(row.id()).id();
                refresh();
                open(copyId);
            } catch (RuntimeException ex) {
                DialogUtil.showError(ex);
            }
        });
    }

    @FXML
    private void onPdf() {
        selected().ifPresent(row -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(DialogUtil.message("quote.pdf.saveTitle"));
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
            chooser.setInitialFileName(DialogUtil.message("quote.pdf.fileName", row.quoteNo().replace('/', '-')));
            File file = chooser.showSaveDialog(window());
            if (file == null) {
                return;
            }
            taskRunner.run(() -> {
                quoteService.exportPdf(row.id(), file.toPath());
                return file;
            }, done -> DialogUtil.showInfo("quote.pdf.success"), loadingIndicator, quoteTable);
        });
    }

    /** "Seçilenleri Sil" for quotes (their lines go with them). */
    private void deleteSelected(List<QuoteRow> rows) {
        BulkDeleteFlow.of(rows, QuoteRow::id, row -> row.quoteNo() + " – " + row.companyName())
                .itemCount("bulk.count.quotes")
                .impact(quoteService::bulkDeletionImpact)
                .delete(quoteService::deleteAll)
                .afterwards(this::refresh)
                .run();
    }

    @FXML
    private void onDelete() {
        selected().ifPresent(row -> {
            if (!DeleteConfirmation.confirmSingle(DialogUtil.message("delete.single.quote", row.quoteNo(),
                    row.companyName()))) {
                return;
            }
            try {
                quoteService.delete(row.id());
            } catch (RuntimeException ex) {
                DialogUtil.showError(ex);
            }
            refresh();
        });
    }

    private Optional<QuoteRow> selected() {
        QuoteRow row = quoteTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            DialogUtil.showErrorMessage("error.selection.required");
        }
        return Optional.ofNullable(row);
    }

    private Window window() {
        return quoteTable.getScene().getWindow();
    }

    /** "12.10.2026" and, for a sent quote past that date, a yellow "Süresi doldu" badge. */
    private static final class ValidityCell extends TableCell<QuoteRow, QuoteRow> {
        @Override
        protected void updateItem(QuoteRow row, boolean empty) {
            super.updateItem(row, empty);
            if (empty || row == null) {
                setGraphic(null);
                return;
            }
            HBox box = new HBox(BADGE_SPACING, new Label(Bicimlendirici.date(row.validUntil())));
            box.setAlignment(Pos.CENTER_LEFT);
            if (row.expired()) {
                Label badge = new Label(DialogUtil.message("quote.badge.expired"));
                badge.getStyleClass().add("warning-badge");
                box.getChildren().add(badge);
            }
            setGraphic(box);
        }
    }
}
