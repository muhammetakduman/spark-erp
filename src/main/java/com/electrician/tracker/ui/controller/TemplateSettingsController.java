package com.electrician.tracker.ui.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import com.electrician.tracker.domain.TemplateType;
import com.electrician.tracker.dto.QuoteLine;
import com.electrician.tracker.dto.TemplateView;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.QuoteCalculator;
import com.electrician.tracker.service.QuoteFieldLimits;
import com.electrician.tracker.service.QuoteLineNumbering;
import com.electrician.tracker.service.TemplateService;
import com.electrician.tracker.ui.util.AppIcon;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.BulkDeleteFlow;
import com.electrician.tracker.ui.util.BulkSelection;
import com.electrician.tracker.ui.util.DeleteConfirmation;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EmptyState;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.QuoteLayoutDiagram;
import com.electrician.tracker.ui.util.TableSorting;
import com.electrician.tracker.ui.util.TextLimits;
import com.electrician.tracker.ui.util.ViewPaths;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Şablonlar" in Settings. On the left a small drawing of the quote PDF shows
 * where the selected template ends up; on the right the list and an editor:
 * "Teklif Şartları" as text with a live preview in the PDF's font (and
 * "Varsayılan yap"), "Hazır Malzeme Listesi" as a table of lines with a
 * total, "Hazır İş Tanımı" as text. Above each: name, last use, use count.
 */
@Component
@Scope("prototype")
public class TemplateSettingsController {

    private final TemplateService templateService;
    private final ModalStageOpener modalStageOpener;
    private final AccessControl accessControl;
    private final QuoteLayoutDiagram diagram = new QuoteLayoutDiagram();
    private final ObservableList<QuoteLine> items = FXCollections.observableArrayList();

    @FXML
    private StackPane diagramHolder;
    @FXML
    private TableView<TemplateView> templateTable;
    @FXML
    private TableColumn<TemplateView, String> nameColumn;
    @FXML
    private TableColumn<TemplateView, String> typeColumn;
    @FXML
    private TableColumn<TemplateView, String> defaultColumn;
    @FXML
    private TableColumn<TemplateView, String> usageColumn;
    @FXML
    private Label noSelectionLabel;
    @FXML
    private VBox editorBox;
    @FXML
    private Label editorTitleLabel;
    @FXML
    private Label editorMetaLabel;
    @FXML
    private Label editorTypeLabel;
    @FXML
    private TextField nameField;
    @FXML
    private CheckBox defaultCheckBox;
    @FXML
    private VBox textEditorBox;
    @FXML
    private TextArea contentArea;
    @FXML
    private Label contentCounter;
    @FXML
    private VBox previewBox;
    @FXML
    private Label previewText;
    @FXML
    private VBox itemEditorBox;
    @FXML
    private TableView<QuoteLine> itemTable;
    @FXML
    private TableColumn<QuoteLine, String> itemProductColumn;
    @FXML
    private TableColumn<QuoteLine, String> itemQuantityColumn;
    @FXML
    private TableColumn<QuoteLine, String> itemUnitPriceColumn;
    @FXML
    private TableColumn<QuoteLine, String> itemTotalColumn;
    @FXML
    private Label itemTotalLabel;

    private TemplateView editing;

    public TemplateSettingsController(TemplateService templateService, ModalStageOpener modalStageOpener,
            AccessControl accessControl) {
        this.templateService = templateService;
        this.modalStageOpener = modalStageOpener;
        this.accessControl = accessControl;
    }

    @FXML
    private void initialize() {
        diagramHolder.getChildren().setAll(diagram.node());
        TableSorting.text(nameColumn, TemplateView::name);
        TableSorting.text(typeColumn, template -> EnumLabels.label(template.type()));
        TableSorting.text(defaultColumn, template -> template.defaultTemplate() ? DialogUtil.message("common.yes") : "");
        TableSorting.text(usageColumn, TemplateSettingsController::usageText);
        templateTable.setPlaceholder(EmptyState.of(AppIcon.TEMPLATE, "template.empty"));
        templateTable.getSelectionModel().selectedItemProperty().addListener((obs, old, template) -> edit(template));
        if (accessControl.isAdmin()) {
            BulkSelection.forTable(templateTable, TemplateView::id, this::deleteSelected);
        }
        setUpItemTable();
        TextLimits.attach(contentArea, contentCounter, QuoteFieldLimits.NOTES);
        previewText.textProperty().bind(contentArea.textProperty());
        defaultCheckBox.setOnAction(event -> toggleDefault());
        refresh(null);
    }

    private void setUpItemTable() {
        itemTable.setItems(items);
        itemProductColumn.setCellValueFactory(cell -> new SimpleStringProperty(productText(cell.getValue())));
        itemQuantityColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                Bicimlendirici.quantity(cell.getValue().quantity()) + " " + EnumLabels.label(cell.getValue().unit())));
        itemUnitPriceColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                moneyOrDash(cell.getValue().unitPrice())));
        itemTotalColumn.setCellValueFactory(cell -> new SimpleStringProperty(moneyOrDash(
                QuoteCalculator.lineTotal(cell.getValue().quantity(), cell.getValue().unitPrice()))));
        for (TableColumn<QuoteLine, String> column : List.of(itemQuantityColumn, itemUnitPriceColumn,
                itemTotalColumn)) {
            column.getStyleClass().add("money-cell");
        }
        itemTable.setPlaceholder(new Label(DialogUtil.message("template.editor.noItems")));
    }

    private void refresh(Long selectId) {
        List<TemplateView> templates = templateService.findAll();
        templateTable.getItems().setAll(templates);
        templates.stream().filter(template -> template.id().equals(selectId)).findFirst()
                .ifPresentOrElse(templateTable.getSelectionModel()::select, () -> edit(null));
    }

    /** Fills the editor with the selected template and highlights its place in the quote. */
    private void edit(TemplateView template) {
        editing = template;
        boolean shown = template != null;
        editorBox.setVisible(shown);
        editorBox.setManaged(shown);
        noSelectionLabel.setVisible(!shown);
        noSelectionLabel.setManaged(!shown);
        diagram.highlight(shown ? template.type() : null);
        if (!shown) {
            return;
        }
        TemplateType type = template.type();
        editorTitleLabel.setText(template.name());
        editorMetaLabel.setText(usageText(template));
        editorTypeLabel.setText(DialogUtil.message("template.type.description." + type.name()));
        nameField.setText(template.name());
        boolean note = type == TemplateType.QUOTE_NOTE;
        show(defaultCheckBox, note);
        defaultCheckBox.setSelected(template.defaultTemplate());
        show(textEditorBox, type.isText());
        show(previewBox, note);
        show(itemEditorBox, !type.isText());
        contentArea.setText(template.content());
        items.setAll(type.isText() ? List.of() : templateService.itemSetLines(template.id()));
        updateItemTotal();
    }

    private static void show(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    /** "Son kullanım: 28.09.2026 · 5 kez kullanıldı" or "Henüz kullanılmadı". */
    private static String usageText(TemplateView template) {
        if (template.lastUsedAt() == null) {
            return DialogUtil.message("template.usage.never");
        }
        return DialogUtil.message("template.usage.text", Bicimlendirici.date(template.lastUsedAt().toLocalDate()),
                template.useCount());
    }

    private static String productText(QuoteLine line) {
        return line.brand() == null ? line.productName() : line.brand() + " " + line.productName();
    }

    private static String moneyOrDash(BigDecimal value) {
        return value == null ? "—" : Bicimlendirici.money(value);
    }

    private void updateItemTotal() {
        BigDecimal total = items.stream().map(line -> QuoteCalculator.lineTotal(line.quantity(), line.unitPrice()))
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        itemTotalLabel.setText(DialogUtil.message("template.editor.total", Bicimlendirici.money(total)));
    }

    // ---- Actions ---------------------------------------------------------------

    @FXML
    private void onNew() {
        TemplateFormController form = modalStageOpener.openAndWait(ViewPaths.TEMPLATE_FORM, "template.dialog.new",
                templateTable.getScene().getWindow());
        if (form.isSaved()) {
            refresh(form.savedId());
        }
    }

    @FXML
    private void onSave() {
        if (editing == null) {
            return;
        }
        try {
            TemplateView saved = editing.type().isText()
                    ? templateService.saveText(editing.id(), nameField.getText(), editing.type(), contentArea.getText())
                    : templateService.updateItemSet(editing.id(), nameField.getText(), List.copyOf(items));
            refresh(saved.id());
            DialogUtil.showSuccess("template.saved");
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    /** "Varsayılan yap" — only for "Teklif Şartları"; applies at once. */
    private void toggleDefault() {
        if (editing == null) {
            return;
        }
        try {
            templateService.setDefault(editing.id(), defaultCheckBox.isSelected());
            refresh(editing.id());
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    @FXML
    private void onDelete() {
        if (editing == null
                || !DeleteConfirmation.confirmSingle(DialogUtil.message("delete.single.template", editing.name()))) {
            return;
        }
        try {
            templateService.delete(editing.id());
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        refresh(null);
    }

    private void deleteSelected(List<TemplateView> templates) {
        BulkDeleteFlow.of(templates, TemplateView::id, TemplateView::name)
                .itemCount("bulk.count.templates")
                .delete(templateService::deleteAll)
                .afterwards(() -> refresh(null))
                .run();
    }

    @FXML
    private void onAddItem() {
        QuoteItemDialogController dialog = modalStageOpener.openAndWait(ViewPaths.QUOTE_ITEM_DIALOG,
                "quote.item.dialog.new", templateTable.getScene().getWindow());
        dialog.getResult().ifPresent(line -> {
            items.setAll(QuoteLineNumbering.append(List.copyOf(items), line));
            updateItemTotal();
        });
    }

    @FXML
    private void onRemoveItem() {
        int index = itemTable.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        items.setAll(QuoteLineNumbering.remove(List.copyOf(items), index));
        updateItemTotal();
    }
}
