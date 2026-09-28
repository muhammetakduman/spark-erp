package com.electrician.tracker.ui.controller;

import java.util.Optional;

import com.electrician.tracker.domain.TemplateType;
import com.electrician.tracker.dto.TemplateView;
import com.electrician.tracker.service.TemplateService;
import com.electrician.tracker.ui.util.AppIcon;
import com.electrician.tracker.ui.util.DeleteConfirmation;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EmptyState;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.TableSorting;
import com.electrician.tracker.ui.util.ViewPaths;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Şablonlar" in Settings: quote notes, quote item sets and daily job titles.
 * Text templates are written and changed here; item sets are saved from a
 * quote and can only be renamed. One note template can be the default of
 * new quotes.
 */
@Component
@Scope("prototype")
public class TemplateSettingsController {

    private static final int PREVIEW_LENGTH = 70;
    private static final String ELLIPSIS = "…";

    private final TemplateService templateService;
    private final ModalStageOpener modalStageOpener;

    @FXML
    private TableView<TemplateView> templateTable;
    @FXML
    private TableColumn<TemplateView, String> nameColumn;
    @FXML
    private TableColumn<TemplateView, String> typeColumn;
    @FXML
    private TableColumn<TemplateView, String> defaultColumn;
    @FXML
    private TableColumn<TemplateView, String> contentColumn;

    public TemplateSettingsController(TemplateService templateService, ModalStageOpener modalStageOpener) {
        this.templateService = templateService;
        this.modalStageOpener = modalStageOpener;
    }

    @FXML
    private void initialize() {
        TableSorting.text(nameColumn, TemplateView::name);
        TableSorting.text(typeColumn, template -> EnumLabels.label(template.type()));
        TableSorting.text(defaultColumn, template -> template.defaultTemplate() ? DialogUtil.message("common.yes") : "");
        TableSorting.text(contentColumn, TemplateSettingsController::preview);
        templateTable.setPlaceholder(EmptyState.of(AppIcon.TEMPLATE, "template.empty"));
        refresh();
    }

    private void refresh() {
        templateTable.getItems().setAll(templateService.findAll());
    }

    private static String preview(TemplateView template) {
        if (template.type() == TemplateType.QUOTE_ITEM_SET) {
            return DialogUtil.message("template.itemSet.lines", template.lineCount());
        }
        String oneLine = template.content().replaceAll("\\s+", " ").trim();
        return oneLine.length() > PREVIEW_LENGTH ? oneLine.substring(0, PREVIEW_LENGTH) + ELLIPSIS : oneLine;
    }

    @FXML
    private void onNew() {
        TemplateFormController form = modalStageOpener.openAndWait(ViewPaths.TEMPLATE_FORM, "template.dialog.new",
                templateTable.getScene().getWindow());
        if (form.isSaved()) {
            refresh();
        }
    }

    @FXML
    private void onEdit() {
        selected().ifPresent(template -> {
            TemplateFormController form = modalStageOpener.openAndWait(ViewPaths.TEMPLATE_FORM,
                    "template.dialog.edit", templateTable.getScene().getWindow(), c -> c.editExisting(template));
            if (form.isSaved()) {
                refresh();
            }
        });
    }

    @FXML
    private void onToggleDefault() {
        selected().ifPresent(template -> {
            try {
                templateService.setDefault(template.id(), !template.defaultTemplate());
                refresh();
            } catch (RuntimeException ex) {
                DialogUtil.showError(ex);
            }
        });
    }

    @FXML
    private void onDelete() {
        selected().ifPresent(template -> {
            if (!DeleteConfirmation.confirmSingle(DialogUtil.message("delete.single.template", template.name()))) {
                return;
            }
            try {
                templateService.delete(template.id());
            } catch (RuntimeException ex) {
                DialogUtil.showError(ex);
            }
            refresh();
        });
    }

    private Optional<TemplateView> selected() {
        TemplateView template = templateTable.getSelectionModel().getSelectedItem();
        if (template == null) {
            DialogUtil.showErrorMessage("error.selection.required");
        }
        return Optional.ofNullable(template);
    }
}
