package com.electrician.tracker.ui.controller;

import java.util.Arrays;

import com.electrician.tracker.domain.TemplateType;
import com.electrician.tracker.dto.TemplateView;
import com.electrician.tracker.service.QuoteFieldLimits;
import com.electrician.tracker.service.TemplateService;
import com.electrician.tracker.service.messaging.MessagePlaceholders;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.TextLimits;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Writes or changes a text template (quote note, daily job title or a message
 * for customers). An item set can only be renamed here; its lines come from a
 * quote.
 *
 * <p>A message template is written once and sent to many people, so the names
 * it can carry ("{musteri}", "{bakiye}") are offered as buttons that drop the
 * placeholder in at the cursor: nobody should have to remember their spelling,
 * and one wrong letter would travel into every message.
 */
@Component
@Scope("prototype")
public class TemplateFormController {

    private final TemplateService templateService;

    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<TemplateType> typeComboBox;
    @FXML
    private VBox contentBox;
    @FXML
    private TextArea contentArea;
    @FXML
    private Label contentCounter;
    @FXML
    private VBox placeholderBox;
    @FXML
    private FlowPane placeholderPane;
    @FXML
    private Button saveButton;

    private TemplateView editing;
    private boolean saved;
    private Long savedId;

    public TemplateFormController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @FXML
    private void initialize() {
        typeComboBox.getItems().setAll(Arrays.stream(TemplateType.values()).filter(TemplateType::isText).toList());
        typeComboBox.setConverter(new TypeConverter());
        typeComboBox.setValue(TemplateType.QUOTE_NOTE);
        TextLimits.attach(contentArea, contentCounter, QuoteFieldLimits.NOTES);
        buildPlaceholderButtons();
        typeComboBox.valueProperty().addListener((observable, previous, type) -> showPlaceholders(type));
        showPlaceholders(typeComboBox.getValue());
    }

    public void editExisting(TemplateView template) {
        this.editing = template;
        nameField.setText(template.name());
        if (!typeComboBox.getItems().contains(template.type())) {
            typeComboBox.getItems().add(template.type());
        }
        typeComboBox.setValue(template.type());
        typeComboBox.setDisable(true);
        boolean text = template.type().isText();
        contentBox.setVisible(text);
        contentBox.setManaged(text);
        contentArea.setText(template.content());
        showPlaceholders(template.type());
    }

    /** Only a message carries a customer's details; the other templates have none. */
    private void showPlaceholders(TemplateType type) {
        boolean message = type == TemplateType.MESSAGE;
        placeholderBox.setVisible(message);
        placeholderBox.setManaged(message);
    }

    private void buildPlaceholderButtons() {
        for (String name : MessagePlaceholders.ALL) {
            String placeholder = "{" + name + "}";
            Button button = new Button(placeholder);
            button.getStyleClass().add("placeholder-chip");
            button.setTooltip(new Tooltip(DialogUtil.message("template.placeholder." + name)));
            button.setOnAction(event -> insertAtCaret(placeholder));
            placeholderPane.getChildren().add(button);
        }
    }

    /** Dropped where the cursor sits, so it lands inside the sentence being written. */
    private void insertAtCaret(String placeholder) {
        contentArea.insertText(contentArea.getCaretPosition(), placeholder);
        contentArea.requestFocus();
    }

    public boolean isSaved() {
        return saved;
    }

    /** Id of the saved template, to select it in the list. */
    public Long savedId() {
        return savedId;
    }

    @FXML
    private void onSave() {
        try {
            TemplateView result = editing != null && !editing.type().isText()
                    ? templateService.renameTemplate(editing.id(), nameField.getText())
                    : templateService.saveText(editing == null ? null : editing.id(), nameField.getText(),
                            typeComboBox.getValue(), contentArea.getText());
            savedId = result.id();
            saved = true;
            close();
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }

    private static final class TypeConverter extends StringConverter<TemplateType> {
        @Override
        public String toString(TemplateType type) {
            return type == null ? "" : EnumLabels.label(type);
        }

        @Override
        public TemplateType fromString(String text) {
            return null;
        }
    }
}
