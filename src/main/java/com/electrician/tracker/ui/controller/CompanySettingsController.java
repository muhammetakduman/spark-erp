package com.electrician.tracker.ui.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;
import java.util.Locale;

import com.electrician.tracker.domain.Company;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.CompanyService;
import com.electrician.tracker.ui.util.AppIcon;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.IconSize;
import com.electrician.tracker.ui.util.Icons;
import com.electrician.tracker.ui.util.UnsavedChangesAware;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Firma bilgileri" in Settings (ADMIN only): name, slogan, address, contact
 * details, tax office/number, brand colour and logo, printed at the top of
 * customer PDFs. The logo is dropped onto (or clicked into) a dashed area;
 * PNG, JPG and SVG are accepted and shrunk to 400 px before saving. A small
 * box shows how the PDF letterhead will look.
 */
@Component
@Scope("prototype")
public class CompanySettingsController implements UnsavedChangesAware {

    private static final String DROP_ACTIVE = "logo-drop-zone-active";
    private static final String HEX_FORMAT = "#%02X%02X%02X";
    private static final int COLOR_CHANNEL_MAX = 255;

    private final CompanyService companyService;
    private final AccessControl accessControl;

    @FXML
    private VBox companySection;
    @FXML
    private TextField nameField;
    @FXML
    private TextField sloganField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField webField;
    @FXML
    private TextField taxOfficeField;
    @FXML
    private TextField taxNoField;
    @FXML
    private ColorPicker brandColorPicker;
    @FXML
    private StackPane logoDropZone;
    @FXML
    private VBox logoPlaceholder;
    @FXML
    private Label placeholderIconLabel;
    @FXML
    private ImageView logoView;
    @FXML
    private ProgressIndicator logoProgress;
    @FXML
    private Button removeLogoButton;
    @FXML
    private ImageView previewLogoView;
    @FXML
    private Label previewNameLabel;
    @FXML
    private Label previewAddressLabel;

    private final SimpleBooleanProperty hasLogo = new SimpleBooleanProperty(false);
    private byte[] logo;
    private boolean dirty;

    public CompanySettingsController(CompanyService companyService, AccessControl accessControl) {
        this.companyService = companyService;
        this.accessControl = accessControl;
    }

    @FXML
    private void initialize() {
        boolean admin = accessControl.isAdmin();
        companySection.setVisible(admin);
        companySection.setManaged(admin);
        if (!admin) {
            return;
        }
        placeholderIconLabel.setGraphic(Icons.of(AppIcon.IMAGE, IconSize.EMPTY));
        removeLogoButton.setGraphic(Icons.of(AppIcon.REMOVE, IconSize.BUTTON));
        removeLogoButton.visibleProperty().bind(logoDropZone.hoverProperty().and(hasLogo));
        logoPlaceholder.visibleProperty().bind(hasLogo.not());
        setUpDropZone();
        setUpPreview();
        show(companyService.get());
        trackChanges();
    }

    @Override
    public boolean hasUnsavedChanges() {
        return dirty;
    }

    private void show(Company company) {
        nameField.setText(company.getName());
        sloganField.setText(company.getSlogan());
        addressField.setText(company.getAddress());
        phoneField.setText(company.getPhone());
        emailField.setText(company.getEmail());
        webField.setText(company.getWeb());
        taxOfficeField.setText(company.getTaxOffice());
        taxNoField.setText(company.getTaxNo());
        brandColorPicker.setValue(Color.web(CompanyService.brandColorOf(company)));
        showLogo(company.hasLogo() ? company.getLogo() : null);
        dirty = false;
    }

    private void showLogo(byte[] bytes) {
        this.logo = bytes;
        Image image = bytes == null ? null : new Image(new ByteArrayInputStream(bytes));
        logoView.setImage(image);
        previewLogoView.setImage(image);
        previewLogoView.setVisible(image != null);
        previewLogoView.setManaged(image != null);
        hasLogo.set(bytes != null);
    }

    private void trackChanges() {
        for (TextField field : List.of(nameField, sloganField, addressField, phoneField, emailField, webField,
                taxOfficeField, taxNoField)) {
            field.textProperty().addListener((obs, o, n) -> dirty = true);
        }
        brandColorPicker.valueProperty().addListener((obs, o, n) -> dirty = true);
    }

    /** Letterhead preview: logo, name in the brand colour, address line. */
    private void setUpPreview() {
        previewNameLabel.textProperty().bind(Bindings.createStringBinding(
                () -> nameField.getText() == null || nameField.getText().isBlank()
                        ? DialogUtil.message("company.logo.previewNoName") : nameField.getText(),
                nameField.textProperty()));
        previewAddressLabel.textProperty().bind(addressField.textProperty());
        previewNameLabel.textFillProperty().bind(brandColorPicker.valueProperty());
    }

    private void setUpDropZone() {
        logoDropZone.setOnMouseClicked(event -> {
            if (!removeLogoButton.isHover()) {
                chooseLogo();
            }
        });
        logoDropZone.setOnDragOver(this::onDragOver);
        logoDropZone.setOnDragExited(event -> logoDropZone.getStyleClass().remove(DROP_ACTIVE));
        logoDropZone.setOnDragDropped(this::onDragDropped);
    }

    private void onDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
            if (!logoDropZone.getStyleClass().contains(DROP_ACTIVE)) {
                logoDropZone.getStyleClass().add(DROP_ACTIVE);
            }
        }
        event.consume();
    }

    private void onDragDropped(DragEvent event) {
        logoDropZone.getStyleClass().remove(DROP_ACTIVE);
        List<File> files = event.getDragboard().getFiles();
        event.setDropCompleted(!files.isEmpty());
        event.consume();
        if (!files.isEmpty()) {
            loadLogo(files.get(0));
        }
    }

    private void chooseLogo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(DialogUtil.message("company.action.chooseLogo"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                DialogUtil.message("company.logo.fileFilter"), "*.png", "*.jpg", "*.jpeg", "*.svg"));
        File file = chooser.showOpenDialog(logoDropZone.getScene().getWindow());
        if (file != null) {
            loadLogo(file);
        }
    }

    /** Shrinks the image in the background; another file type only gets a warning. */
    private void loadLogo(File file) {
        if (!companyService.isAcceptedLogo(file.getName())) {
            DialogUtil.showErrorMessage("error.company.logo.type");
            return;
        }
        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() {
                return companyService.prepareLogo(file.toPath());
            }
        };
        logoProgress.visibleProperty().bind(task.runningProperty());
        task.setOnSucceeded(event -> {
            showLogo(task.getValue());
            dirty = true;
        });
        task.setOnFailed(event -> DialogUtil.showError(task.getException() instanceof RuntimeException runtime
                ? runtime : new IllegalStateException(task.getException())));
        Thread thread = new Thread(task, "logo");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onRemoveLogo() {
        showLogo(null);
        dirty = true;
    }

    @FXML
    private void onSave() {
        Company changes = Company.empty();
        changes.setName(nameField.getText());
        changes.setSlogan(sloganField.getText());
        changes.setAddress(addressField.getText());
        changes.setPhone(phoneField.getText());
        changes.setEmail(emailField.getText());
        changes.setWeb(webField.getText());
        changes.setTaxOffice(taxOfficeField.getText());
        changes.setTaxNo(taxNoField.getText());
        changes.setLogo(logo);
        changes.setBrandColor(toHex(brandColorPicker.getValue()));
        try {
            show(companyService.save(changes));
            DialogUtil.showInfo("company.saved");
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    private static String toHex(Color color) {
        if (color == null) {
            return null;
        }
        return String.format(Locale.ROOT, HEX_FORMAT, channel(color.getRed()), channel(color.getGreen()),
                channel(color.getBlue()));
    }

    private static int channel(double value) {
        return (int) Math.round(value * COLOR_CHANNEL_MAX);
    }
}
