package com.electrician.tracker.ui.controller;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.DiscountType;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.QuoteStatus;
import com.electrician.tracker.domain.TemplateType;
import com.electrician.tracker.dto.ItemSetApplication;
import com.electrician.tracker.dto.QuoteDraft;
import com.electrician.tracker.dto.QuoteLine;
import com.electrician.tracker.dto.QuoteTotals;
import com.electrician.tracker.dto.QuoteView;
import com.electrician.tracker.dto.TemplateView;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.CustomerService;
import com.electrician.tracker.service.QuoteCalculator;
import com.electrician.tracker.service.QuoteConversionService;
import com.electrician.tracker.service.QuoteFieldLimits;
import com.electrician.tracker.service.QuoteLineNumbering;
import com.electrician.tracker.service.QuoteService;
import com.electrician.tracker.service.TemplateService;
import com.electrician.tracker.ui.util.AppIcon;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.CustomerNameField;
import com.electrician.tracker.ui.util.DecimalField;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EmptyState;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.JobNavigator;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.TextLimits;
import com.electrician.tracker.ui.util.ViewPaths;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Teklif hazırlama": the customer block (a listed customer fills address,
 * phone and e-mail; a new name can be typed and optionally added to the
 * customer list), numbering and validity, the lines ordered only by their
 * number (move up/down with the arrow buttons or Ctrl+↑/↓, insert, remove,
 * add from an item-set template; at most 25 so the PDF fits one page),
 * discount, labor and VAT with live totals, notes from note templates, and
 * the PDF. Texts stop at the lengths that fit the PDF. An ADMIN can turn an
 * accepted quote into a site or service job. Nothing here shows cost or
 * profit.
 */
@Component
@Scope("prototype")
public class QuoteFormController {

    private static final int NO_VAT = 0;
    private static final List<Integer> VAT_CHOICES = List.of(NO_VAT, 1, 10, 20);
    private static final int MAX_VALIDITY_DAYS = 365;
    private static final int DOUBLE_CLICK = 2;

    private final QuoteService quoteService;
    private final QuoteConversionService quoteConversionService;
    private final CustomerService customerService;
    private final TemplateService templateService;
    private final AccessControl accessControl;
    private final ModalStageOpener modalStageOpener;
    private final JobNavigator jobNavigator;

    @FXML
    private VBox rootContent;
    @FXML
    private ComboBox<String> companyNameComboBox;
    @FXML
    private CheckBox addCustomerCheckBox;
    @FXML
    private Label customerMatchLabel;
    @FXML
    private Label companyNameCounter;
    @FXML
    private TextArea addressArea;
    @FXML
    private Label addressCounter;
    @FXML
    private TextField contactPersonField;
    @FXML
    private Label contactPersonCounter;
    @FXML
    private TextField phoneField;
    @FXML
    private Label phoneCounter;
    @FXML
    private TextField faxField;
    @FXML
    private Label faxCounter;
    @FXML
    private TextField emailField;
    @FXML
    private Label emailCounter;
    @FXML
    private TextField subjectField;
    @FXML
    private Label subjectCounter;
    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField numberField;
    @FXML
    private Spinner<Integer> validitySpinner;
    @FXML
    private ComboBox<QuoteStatus> statusComboBox;
    @FXML
    private Label lineCountLabel;
    @FXML
    private Label smallFontNotice;
    @FXML
    private TableView<QuoteLine> itemTable;
    @FXML
    private TableColumn<QuoteLine, String> lineNoColumn;
    @FXML
    private TableColumn<QuoteLine, String> brandColumn;
    @FXML
    private TableColumn<QuoteLine, String> productColumn;
    @FXML
    private TableColumn<QuoteLine, String> quantityColumn;
    @FXML
    private TableColumn<QuoteLine, String> unitColumn;
    @FXML
    private TableColumn<QuoteLine, String> unitPriceColumn;
    @FXML
    private TableColumn<QuoteLine, String> totalColumn;
    @FXML
    private Button moveUpButton;
    @FXML
    private Button moveDownButton;
    @FXML
    private Button addItemButton;
    @FXML
    private Button insertItemButton;
    @FXML
    private Label lineLimitLabel;
    @FXML
    private ComboBox<TemplateView> noteTemplateComboBox;
    @FXML
    private TextArea notesArea;
    @FXML
    private Label notesCounter;
    @FXML
    private ComboBox<DiscountType> discountTypeComboBox;
    @FXML
    private DecimalField discountField;
    @FXML
    private DecimalField laborField;
    @FXML
    private ComboBox<Integer> vatComboBox;
    @FXML
    private GridPane totalsGrid;
    @FXML
    private Label preparedByLabel;
    @FXML
    private Hyperlink jobLink;
    @FXML
    private Button convertButton;

    private final ObservableList<QuoteLine> lines = FXCollections.observableArrayList();
    private CustomerNameField customerField;
    private QuoteView view;
    private boolean changed;

    public QuoteFormController(QuoteService quoteService, QuoteConversionService quoteConversionService,
            CustomerService customerService, TemplateService templateService, AccessControl accessControl,
            ModalStageOpener modalStageOpener, JobNavigator jobNavigator) {
        this.quoteService = quoteService;
        this.quoteConversionService = quoteConversionService;
        this.customerService = customerService;
        this.templateService = templateService;
        this.accessControl = accessControl;
        this.modalStageOpener = modalStageOpener;
        this.jobNavigator = jobNavigator;
    }

    @FXML
    private void initialize() {
        setUpCustomerField();
        setUpLimits();
        validitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, MAX_VALIDITY_DAYS,
                QuoteService.DEFAULT_VALIDITY_DAYS));
        statusComboBox.getItems().setAll(QuoteStatus.values());
        statusComboBox.setConverter(new NameConverter<>(EnumLabels::label));
        statusComboBox.valueProperty().addListener((obs, o, n) -> refreshConvertButton());
        discountTypeComboBox.getItems().setAll(DiscountType.values());
        discountTypeComboBox.setConverter(new NameConverter<>(EnumLabels::label));
        discountTypeComboBox.valueProperty().addListener((obs, o, type) -> onDiscountTypeChanged(type));
        vatComboBox.getItems().setAll(VAT_CHOICES);
        vatComboBox.setConverter(new NameConverter<>(QuoteFormController::vatLabel));
        setUpItemTable();
        setUpNoteTemplates();
        for (var field : List.of(discountField, laborField)) {
            field.textProperty().addListener((obs, o, n) -> refreshTotals());
        }
        vatComboBox.valueProperty().addListener((obs, o, n) -> refreshTotals());
        convertButton.setVisible(accessControl.isAdmin());
        convertButton.setManaged(accessControl.isAdmin());
    }

    /** A new quote: next number, today, 15 days, 20 % VAT, the default note template. */
    public void startNew() {
        show(quoteService.newQuote(DialogUtil.message("quote.notes.default")));
    }

    public void editExisting(Long quoteId) {
        show(quoteService.findById(quoteId));
    }

    /** Whether anything was saved, so the list reloads. */
    public boolean isChanged() {
        return changed;
    }

    // ---- Customer --------------------------------------------------------------

    /**
     * A listed customer fills address, phone and e-mail; for any other name
     * "add to the customer list" appears (ticked by default).
     */
    private void setUpCustomerField() {
        customerField = new CustomerNameField(companyNameComboBox, customerService.findAll());
        customerField.setOnCustomerChosen(this::fillFromCustomer);
        customerField.matchedCustomerProperty().addListener((obs, o, n) -> refreshCustomerHint());
        companyNameComboBox.getEditor().textProperty().addListener((obs, o, n) -> refreshCustomerHint());
    }

    private void fillFromCustomer(Customer customer) {
        addressArea.setText(customer.getAddress());
        phoneField.setText(customer.getPhone());
        emailField.setText(customer.getEmail());
    }

    private void refreshCustomerHint() {
        Customer matched = customerField.matchedCustomer();
        boolean typedNew = matched == null && !customerField.typedName().isEmpty();
        setShown(addCustomerCheckBox, typedNew);
        setShown(customerMatchLabel, matched != null);
        if (matched != null) {
            customerMatchLabel.setText(DialogUtil.message("quote.customer.linked", matched.getName()));
        }
    }

    private void setUpLimits() {
        TextLimits.attach(companyNameComboBox.getEditor(), companyNameCounter, QuoteFieldLimits.COMPANY_NAME);
        TextLimits.attach(addressArea, addressCounter, QuoteFieldLimits.ADDRESS);
        TextLimits.attach(contactPersonField, contactPersonCounter, QuoteFieldLimits.CONTACT_PERSON);
        TextLimits.attach(phoneField, phoneCounter, QuoteFieldLimits.PHONE);
        TextLimits.attach(faxField, faxCounter, QuoteFieldLimits.PHONE);
        TextLimits.attach(emailField, emailCounter, QuoteFieldLimits.EMAIL);
        TextLimits.attach(subjectField, subjectCounter, QuoteFieldLimits.SUBJECT);
        TextLimits.attach(notesArea, notesCounter, QuoteFieldLimits.NOTES);
    }

    private void show(QuoteView shown) {
        this.view = shown;
        QuoteDraft draft = shown.draft();
        customerField.showName(draft.companyName());
        addressArea.setText(draft.address());
        contactPersonField.setText(draft.contactPerson());
        phoneField.setText(draft.phone());
        faxField.setText(draft.fax());
        emailField.setText(draft.email());
        subjectField.setText(draft.subject());
        datePicker.setValue(draft.quoteDate());
        numberField.setText(draft.quoteNo());
        validitySpinner.getValueFactory().setValue(draft.validityDays());
        statusComboBox.setValue(draft.status());
        setLines(QuoteLineNumbering.normalize(draft.lines()));
        discountTypeComboBox.setValue(draft.discountType() == null ? DiscountType.NONE : draft.discountType());
        discountField.setValue(draft.discountValue());
        laborField.setValue(draft.laborAmount());
        vatComboBox.setValue(draft.vatRate() == null ? NO_VAT : draft.vatRate());
        notesArea.setText(draft.notes());
        preparedByLabel.setText(DialogUtil.message("quote.preparedBy",
                Objects.requireNonNullElse(shown.preparedByName(), "—"),
                Objects.requireNonNullElse(shown.preparedByTitle(), "")));
        showJobLink();
        refreshTotals();
        refreshConvertButton();
        refreshCustomerHint();
    }

    private void showJobLink() {
        boolean converted = view.isConvertedToJob();
        jobLink.setVisible(converted);
        jobLink.setManaged(converted);
        if (converted) {
            jobLink.setText(DialogUtil.message("quote.convertedLink"));
        }
    }

    @FXML
    private void onSuggestNumber() {
        if (datePicker.getValue() != null) {
            numberField.setText(quoteService.suggestNumber(datePicker.getValue()));
        }
    }

    private void onDiscountTypeChanged(DiscountType type) {
        boolean hasValue = type != null && type != DiscountType.NONE;
        discountField.setDisable(!hasValue);
        if (!hasValue) {
            discountField.clear();
        }
        refreshTotals();
    }

    // ---- Lines ---------------------------------------------------------------

    /**
     * Lines are shown in their number order only; no column sorts them (the
     * one exception to the app's sortable tables). Ctrl+↑/↓ moves a line.
     */
    private void setUpItemTable() {
        itemTable.setItems(lines);
        itemTable.setSortPolicy(table -> false);
        itemTable.setPlaceholder(EmptyState.of(AppIcon.QUOTES, "quote.items.empty"));
        lineNoColumn.setCellValueFactory(d -> text(String.valueOf(d.getValue().lineNo())));
        brandColumn.setCellValueFactory(d -> text(d.getValue().brand()));
        productColumn.setCellValueFactory(d -> text(productText(d.getValue())));
        quantityColumn.setCellValueFactory(d -> text(Bicimlendirici.quantity(d.getValue().quantity())));
        unitColumn.setCellValueFactory(d -> text(EnumLabels.label(d.getValue().unit())));
        unitPriceColumn.setCellValueFactory(d -> text(Bicimlendirici.moneyTl(d.getValue().unitPrice())));
        totalColumn.setCellValueFactory(d -> text(Bicimlendirici.moneyTl(
                QuoteCalculator.lineTotal(d.getValue().quantity(), d.getValue().unitPrice()))));
        for (TableColumn<QuoteLine, String> column : List.of(lineNoColumn, unitPriceColumn, totalColumn,
                quantityColumn)) {
            column.getStyleClass().add("money-cell");
        }
        itemTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == DOUBLE_CLICK && itemTable.getSelectionModel().getSelectedIndex() >= 0) {
                onEditItem();
            }
        });
        itemTable.addEventFilter(KeyEvent.KEY_PRESSED, this::onTableKey);
        moveUpButton.setTooltip(new javafx.scene.control.Tooltip(DialogUtil.message("quote.action.moveUp")));
        moveDownButton.setTooltip(new javafx.scene.control.Tooltip(DialogUtil.message("quote.action.moveDown")));
    }

    private void onTableKey(KeyEvent event) {
        if (!event.isShortcutDown()) {
            return;
        }
        if (event.getCode() == KeyCode.UP) {
            onMoveUp();
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN) {
            onMoveDown();
            event.consume();
        }
    }

    private static String productText(QuoteLine line) {
        return line.description() == null || line.description().isBlank() ? line.productName()
                : line.productName() + " (" + line.description() + ")";
    }

    private static SimpleStringProperty text(String value) {
        return new SimpleStringProperty(value == null ? "" : value);
    }

    /** Shows the lines, then the line count, the 25-line limit and the small-font notice. */
    private void setLines(List<QuoteLine> newLines) {
        lines.setAll(newLines);
        int count = lines.size();
        boolean full = !QuoteLineNumbering.canAdd(count);
        addItemButton.setDisable(full);
        insertItemButton.setDisable(full);
        setShown(lineLimitLabel, full);
        setShown(smallFontNotice, count > QuoteFieldLimits.SMALL_FONT_LINE_COUNT);
        lineCountLabel.setText(DialogUtil.message("quote.items.count", count, QuoteLineNumbering.MAX_LINES));
        refreshTotals();
    }

    /** Applies a numbering change and keeps the line at {@code selectIndex} selected. */
    private void changeLines(UnaryOperator<List<QuoteLine>> change, int selectIndex) {
        try {
            setLines(change.apply(List.copyOf(lines)));
            int index = Math.min(selectIndex, lines.size() - 1);
            if (index >= 0) {
                itemTable.getSelectionModel().clearAndSelect(index);
                itemTable.scrollTo(index);
            }
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    @FXML
    private void onAddItem() {
        openItemDialog(null).ifPresent(line -> changeLines(current -> QuoteLineNumbering.append(current, line),
                lines.size()));
    }

    /** Inserts a new line right above the selected one. */
    @FXML
    private void onInsertItem() {
        int index = itemTable.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            onAddItem();
            return;
        }
        openItemDialog(null).ifPresent(line -> changeLines(current -> QuoteLineNumbering.insertAt(current, index,
                line), index));
    }

    @FXML
    private void onEditItem() {
        int index = selectedIndex();
        if (index < 0) {
            return;
        }
        openItemDialog(lines.get(index)).ifPresent(line -> changeLines(current -> QuoteLineNumbering.replace(
                current, index, line), index));
    }

    @FXML
    private void onMoveUp() {
        int index = selectedIndex();
        if (index > 0) {
            changeLines(current -> QuoteLineNumbering.moveUp(current, index), index - 1);
        }
    }

    @FXML
    private void onMoveDown() {
        int index = selectedIndex();
        if (index >= 0 && index < lines.size() - 1) {
            changeLines(current -> QuoteLineNumbering.moveDown(current, index), index + 1);
        }
    }

    @FXML
    private void onRemoveItem() {
        int index = selectedIndex();
        if (index >= 0) {
            changeLines(current -> QuoteLineNumbering.remove(current, index), index);
        }
    }

    private int selectedIndex() {
        int index = itemTable.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            DialogUtil.showErrorMessage("error.selection.required");
        }
        return index;
    }

    private Optional<QuoteLine> openItemDialog(QuoteLine existing) {
        String titleKey = existing == null ? "quote.item.dialog.new" : "quote.item.dialog.edit";
        QuoteItemDialogController dialog = modalStageOpener.openAndWait(ViewPaths.QUOTE_ITEM_DIALOG, titleKey,
                window(), controller -> {
                    if (existing != null) {
                        controller.editLine(existing);
                    }
                });
        return dialog.getResult();
    }

    // ---- Templates -------------------------------------------------------------

    /** Picking a note template writes its text into the notes. */
    private void setUpNoteTemplates() {
        noteTemplateComboBox.setConverter(new NameConverter<>(TemplateView::name));
        reloadNoteTemplates();
        noteTemplateComboBox.valueProperty().addListener((obs, o, template) -> {
            if (template != null) {
                notesArea.setText(template.content());
            }
        });
    }

    private void reloadNoteTemplates() {
        noteTemplateComboBox.getItems().setAll(templateService.findByType(TemplateType.QUOTE_NOTE));
    }

    @FXML
    private void onSaveNotesAsTemplate() {
        askName("quote.notes.templateName").ifPresent(name -> {
            try {
                templateService.saveText(null, name, TemplateType.QUOTE_NOTE, notesArea.getText());
                reloadNoteTemplates();
                DialogUtil.showInfo("template.saved");
            } catch (RuntimeException ex) {
                DialogUtil.showError(ex);
            }
        });
    }

    /** "Şablondan Ekle": the set's lines go below the existing ones, numbering continues. */
    @FXML
    private void onAddFromTemplate() {
        List<TemplateView> sets = templateService.findByType(TemplateType.QUOTE_ITEM_SET);
        if (sets.isEmpty()) {
            DialogUtil.showInfo("quote.template.noItemSets");
            return;
        }
        ChoiceDialog<TemplateView> dialog = new ChoiceDialog<>(sets.get(0), sets);
        dialog.setTitle(DialogUtil.message("quote.action.fromTemplate"));
        dialog.setHeaderText(null);
        dialog.setContentText(DialogUtil.message("quote.template.choose"));
        dialog.showAndWait().ifPresent(this::applyItemSet);
    }

    private void applyItemSet(TemplateView template) {
        try {
            ItemSetApplication result = templateService.applyItemSet(template.id(), List.copyOf(lines));
            setLines(result.lines());
            if (result.skippedCount() > 0) {
                DialogUtil.showInfoText(DialogUtil.message("quote.template.skipped", result.skippedCount()));
            }
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    @FXML
    private void onSaveItemsAsTemplate() {
        if (lines.isEmpty()) {
            DialogUtil.showErrorMessage("error.template.lines.required");
            return;
        }
        askName("quote.template.itemSetName").ifPresent(name -> {
            try {
                templateService.saveItemSet(name, List.copyOf(lines));
                DialogUtil.showInfo("template.saved");
            } catch (RuntimeException ex) {
                DialogUtil.showError(ex);
            }
        });
    }

    private static Optional<String> askName(String promptKey) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(DialogUtil.message("template.dialog.new"));
        dialog.setHeaderText(null);
        dialog.setContentText(DialogUtil.message(promptKey));
        return dialog.showAndWait().map(String::trim).filter(name -> !name.isEmpty());
    }

    // ---- Totals --------------------------------------------------------------

    private void refreshTotals() {
        if (view == null) {
            return;
        }
        try {
            fillTotals(quoteService.calculateTotals(buildDraft()));
        } catch (NumberFormatException e) {
            totalsGrid.getChildren().clear();
        }
    }

    /** TOPLAM, İSKONTO, İŞÇİLİK, ARA TOPLAM, KDV, GENEL TOPLAM. */
    private void fillTotals(QuoteTotals totals) {
        totalsGrid.getChildren().clear();
        addTotalRow(0, DialogUtil.message("quote.total.items"), totals.itemsTotal(), false);
        addTotalRow(1, DialogUtil.message("quote.total.discount"), totals.discount(), false);
        addTotalRow(2, DialogUtil.message("quote.total.labor"), totals.labor(), false);
        addTotalRow(3, DialogUtil.message("quote.total.subtotal"), totals.subtotal(), false);
        String vatCaption = totals.vatRate() == null ? DialogUtil.message("quote.total.noVat")
                : DialogUtil.message("quote.total.vat", String.valueOf(totals.vatRate()));
        addTotalRow(4, vatCaption, totals.vatAmount(), false);
        addTotalRow(5, DialogUtil.message("quote.total.grand"), totals.grandTotal(), true);
    }

    private void addTotalRow(int row, String caption, BigDecimal amount, boolean emphasized) {
        Label captionLabel = new Label(caption);
        Label amountLabel = new Label(Bicimlendirici.moneyTl(amount));
        amountLabel.getStyleClass().add("vat-summary-amount");
        if (emphasized) {
            captionLabel.getStyleClass().add("vat-summary-total");
            amountLabel.getStyleClass().add("vat-summary-total");
        }
        totalsGrid.addRow(row, captionLabel, amountLabel);
    }

    private QuoteDraft buildDraft() {
        Customer customer = customerField.matchedCustomer();
        Integer vat = vatComboBox.getValue() == null || vatComboBox.getValue() == NO_VAT ? null : vatComboBox.getValue();
        boolean addToList = customer == null && addCustomerCheckBox.isSelected();
        return new QuoteDraft(numberField.getText(), datePicker.getValue(), validitySpinner.getValue(),
                customer == null ? null : customer.getId(), customerField.typedName(), addressArea.getText(),
                contactPersonField.getText(), phoneField.getText(), faxField.getText(), emailField.getText(),
                subjectField.getText(), discountTypeComboBox.getValue(), discountField.getValue(),
                laborField.getValue(), vat, notesArea.getText(), statusComboBox.getValue(), List.copyOf(lines),
                addToList);
    }

    // ---- Actions -------------------------------------------------------------

    @FXML
    private void onSave() {
        if (save()) {
            DialogUtil.showInfo("quote.saved");
        }
    }

    /** Saves the form; false (with the error shown) when it cannot be saved. */
    private boolean save() {
        try {
            QuoteView saved = quoteService.save(view.id(), buildDraft());
            customerField.reload(customerService.findAll());
            show(saved);
            changed = true;
            return true;
        } catch (NumberFormatException e) {
            DialogUtil.showErrorMessage("error.materialItem.number.invalid");
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        return false;
    }

    /** The PDF always shows what is saved, so the form is saved first. */
    @FXML
    private void onPdf() {
        if (!save()) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(DialogUtil.message("quote.pdf.saveTitle"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        chooser.setInitialFileName(DialogUtil.message("quote.pdf.fileName", view.draft().quoteNo().replace('/', '-')));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        try {
            quoteService.exportPdf(view.id(), file.toPath());
            DialogUtil.showInfo("quote.pdf.success");
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    private void refreshConvertButton() {
        boolean convertible = view != null && view.id() != null && !view.isConvertedToJob()
                && statusComboBox.getValue() == QuoteStatus.ACCEPTED;
        convertButton.setDisable(!convertible);
    }

    /** "İşe Dönüştür": asks site or service, then creates the job with the quote's lines. */
    @FXML
    private void onConvert() {
        if (!save()) {
            return;
        }
        askJobType().ifPresent(type -> {
            try {
                quoteConversionService.convertToJob(view.id(), type);
                changed = true;
                show(quoteService.findById(view.id()));
                DialogUtil.showInfo("quote.converted");
            } catch (RuntimeException ex) {
                DialogUtil.showError(ex);
            }
        });
    }

    private Optional<JobType> askJobType() {
        ButtonType site = new ButtonType(EnumLabels.label(JobType.SITE), ButtonBar.ButtonData.YES);
        ButtonType service = new ButtonType(EnumLabels.label(JobType.SERVICE), ButtonBar.ButtonData.NO);
        ButtonType cancel = new ButtonType(DialogUtil.message("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, DialogUtil.message("quote.convert.question"),
                site, service, cancel);
        alert.setHeaderText(null);
        return alert.showAndWait()
                .filter(button -> button != cancel)
                .map(button -> button == site ? JobType.SITE : JobType.SERVICE);
    }

    @FXML
    private void onOpenJob() {
        if (view == null || !view.isConvertedToJob()) {
            return;
        }
        Long jobId = view.jobId();
        JobType jobType = view.jobType();
        onClose();
        jobNavigator.open(jobId, jobType);
    }

    @FXML
    private void onClose() {
        ((Stage) window()).close();
    }

    private Window window() {
        return rootContent.getScene().getWindow();
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    private static String vatLabel(Integer rate) {
        return rate == null || rate == NO_VAT ? DialogUtil.message("vat.option.none")
                : DialogUtil.message("quote.vat.rate", String.valueOf(rate));
    }

    private static final class NameConverter<T> extends StringConverter<T> {

        private final Function<T, String> name;

        private NameConverter(Function<T, String> name) {
            this.name = name;
        }

        @Override
        public String toString(T value) {
            return value == null ? "" : name.apply(value);
        }

        @Override
        public T fromString(String text) {
            return null;
        }
    }
}
