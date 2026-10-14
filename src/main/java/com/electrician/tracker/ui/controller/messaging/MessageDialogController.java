package com.electrician.tracker.ui.controller.messaging;

import java.util.List;
import java.util.Optional;

import com.electrician.tracker.domain.settings.TemplateType;
import com.electrician.tracker.dto.messaging.BulkMessageResult;
import com.electrician.tracker.dto.messaging.MessageTarget;
import com.electrician.tracker.dto.settings.TemplateView;
import com.electrician.tracker.service.messaging.PhoneNumbers;
import com.electrician.tracker.service.settings.TemplateService;
import com.electrician.tracker.service.messaging.BulkMessageSender;
import com.electrician.tracker.service.messaging.MessagePlaceholders;
import com.electrician.tracker.service.messaging.SmsGateway;
import com.electrician.tracker.service.messaging.SmsResult;
import com.electrician.tracker.ui.util.dialog.DialogUtil;
import com.electrician.tracker.ui.util.messaging.SmsLength;
import com.electrician.tracker.ui.util.task.TaskRunner;
import com.electrician.tracker.ui.util.messaging.WhatsAppLauncher;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Mesaj Gönder": picks a ready message, fills the customer's details into it
 * and hands it to WhatsApp or the SMS provider.
 *
 * <p>The number comes from the customer record and is shown so it can be
 * corrected or replaced for someone who has no record yet. The text is always
 * editable before it leaves: a template is a starting point, not a cage.
 *
 * <p>Opened on several customers at once the window works differently, because
 * the text can no longer belong to one of them: the box then holds the
 * template with its placeholders still in it, a line underneath shows how it
 * reads for the first recipient, and each message is filled with its own
 * recipient's details on the way out. WhatsApp cannot do this — it wants a
 * press of send per chat — so only SMS is offered.
 */
@Component
@Scope("prototype")
public class MessageDialogController {

    private final TemplateService templateService;
    private final SmsGateway smsGateway;
    private final BulkMessageSender bulkMessageSender;
    private final TaskRunner taskRunner;

    @FXML
    private TextField recipientField;
    @FXML
    private TextField phoneField;
    @FXML
    private ComboBox<TemplateView> templateComboBox;
    @FXML
    private TextArea messageArea;
    @FXML
    private Label lengthLabel;
    @FXML
    private Label previewLabel;
    @FXML
    private Label phoneWarningLabel;
    @FXML
    private Button whatsAppButton;
    @FXML
    private Button smsButton;
    @FXML
    private ProgressIndicator loadingIndicator;

    private MessageTarget target = MessageTarget.of("", "", null);
    /** Empty for the ordinary one-customer window. */
    private List<MessageTarget> recipients = List.of();

    public MessageDialogController(TemplateService templateService, SmsGateway smsGateway,
            BulkMessageSender bulkMessageSender, TaskRunner taskRunner) {
        this.templateService = templateService;
        this.smsGateway = smsGateway;
        this.bulkMessageSender = bulkMessageSender;
        this.taskRunner = taskRunner;
    }

    @FXML
    private void initialize() {
        templateComboBox.getItems().setAll(templateService.findByType(TemplateType.MESSAGE));
        templateComboBox.valueProperty().addListener((observable, previous, chosen) -> applyTemplate(chosen));
        messageArea.textProperty().addListener((observable, previous, text) -> showLength(text));
        phoneField.textProperty().addListener((observable, previous, text) -> showPhoneState(text));
        if (!smsGateway.isConfigured()) {
            smsButton.setDisable(true);
            smsButton.setTooltip(new Tooltip(DialogUtil.message("message.sms.notConfigured")));
        }
        hide(previewLabel);
        showLength(messageArea.getText());
    }

    /** Opened from a customer or a service row, with that customer's details. */
    public void startFor(MessageTarget messageTarget) {
        this.target = messageTarget;
        recipientField.setText(messageTarget.name());
        phoneField.setText(messageTarget.rawPhone());
        showPhoneState(messageTarget.rawPhone());
        selectDefaultTemplate();
    }

    /**
     * Opened on a whole selection. The first recipient stands in for the
     * preview; nothing is filled into the box itself, so every message keeps
     * its own name and balance.
     */
    public void startForMany(List<MessageTarget> targets) {
        this.recipients = List.copyOf(targets);
        this.target = targets.isEmpty() ? MessageTarget.of("", "", null) : targets.get(0);
        recipientField.setText(DialogUtil.message("message.recipients.count", targets.size()));
        recipientField.setEditable(false);
        phoneField.setText("");
        phoneField.setDisable(true);
        phoneField.setPromptText(DialogUtil.message("message.field.phone.many"));
        hide(phoneWarningLabel);
        whatsAppButton.setDisable(true);
        whatsAppButton.setTooltip(new Tooltip(DialogUtil.message("message.whatsApp.oneAtATime")));
        smsButton.setDisable(!smsGateway.isConfigured());
        selectDefaultTemplate();
        showLength(messageArea.getText());
    }

    private boolean isBulk() {
        return !recipients.isEmpty();
    }

    private void selectDefaultTemplate() {
        templateComboBox.getItems().stream()
                .filter(TemplateView::defaultTemplate)
                .findFirst()
                .ifPresent(templateComboBox::setValue);
    }

    /**
     * One recipient gets the finished text; a selection keeps the placeholders,
     * because each message is filled with its own recipient's details later.
     */
    private void applyTemplate(TemplateView template) {
        if (template == null) {
            return;
        }
        messageArea.setText(isBulk() ? template.content()
                : MessagePlaceholders.fill(template.content(), target.values()));
    }

    /** Turkish letters halve what fits in one SMS, so the count is worth showing. */
    private void showLength(String text) {
        if (isBulk()) {
            showBulkLength(text);
            return;
        }
        int length = text == null ? 0 : text.length();
        lengthLabel.setText(DialogUtil.message("message.length", length, SmsLength.partCount(text)));
    }

    /**
     * For a selection the only figure that matters is what the run will cost,
     * so the count is of every recipient's own parts added up.
     */
    private void showBulkLength(String template) {
        List<MessageTarget> reachable = BulkMessageSender.reachable(recipients);
        int parts = BulkMessageSender.totalParts(template, recipients, SmsLength::partCount);
        lengthLabel.setText(DialogUtil.message("message.length.many", reachable.size(), parts));
        String preview = MessagePlaceholders.fill(template, target.values());
        previewLabel.setText(DialogUtil.message("message.preview", target.name(), preview));
        previewLabel.setVisible(!preview.isBlank());
        previewLabel.setManaged(!preview.isBlank());
    }

    /**
     * A number the program cannot read is not fatal — the user may still copy
     * the text — so it only disables the two send buttons and says why.
     */
    private void showPhoneState(String typed) {
        if (isBulk()) {
            return;
        }
        boolean readable = PhoneNumbers.toWhatsApp(typed).isPresent();
        whatsAppButton.setDisable(!readable);
        smsButton.setDisable(!readable || !smsGateway.isConfigured());
        phoneWarningLabel.setVisible(!readable);
        phoneWarningLabel.setManaged(!readable);
        if (!readable) {
            phoneWarningLabel.setText(DialogUtil.message("message.phone.unreadable"));
        }
    }

    @FXML
    private void onWhatsApp() {
        phoneNumber().ifPresent(phone -> {
            WhatsAppLauncher.Outcome outcome = WhatsAppLauncher.open(phone, messageArea.getText());
            markTemplateUsed();
            switch (outcome) {
                case OPENED -> DialogUtil.showInfo("message.whatsApp.opened");
                case COPIED_TO_CLIPBOARD -> DialogUtil.showInfo("message.whatsApp.copiedInstead");
                case FAILED -> DialogUtil.showErrorMessage("message.whatsApp.failed");
            }
        });
    }

    @FXML
    private void onSms() {
        if (isBulk()) {
            sendToEveryone();
            return;
        }
        Optional<String> phone = phoneNumber();
        if (phone.isEmpty() || !DialogUtil.confirm("message.sms.confirm", phone.get())) {
            return;
        }
        String text = messageArea.getText();
        taskRunner.run(() -> smsGateway.send(phone.get(), text), this::showSmsResult, loadingIndicator, messageArea);
    }

    /**
     * Sending to a list costs real money, so the confirmation names the number
     * of recipients and the number of messages they add up to before anything
     * leaves.
     */
    private void sendToEveryone() {
        String template = messageArea.getText();
        List<MessageTarget> reachable = BulkMessageSender.reachable(recipients);
        if (reachable.isEmpty()) {
            DialogUtil.showErrorMessage("message.bulk.noneReachable");
            return;
        }
        int parts = BulkMessageSender.totalParts(template, recipients, SmsLength::partCount);
        if (!DialogUtil.confirm("message.bulk.confirm", reachable.size(), parts)) {
            return;
        }
        taskRunner.run(() -> bulkMessageSender.send(template, recipients, this::reasonText),
                this::showBulkResult, loadingIndicator, messageArea);
    }

    private void showBulkResult(BulkMessageResult result) {
        if (result.sent() > 0) {
            markTemplateUsed();
        }
        if (result.isCompleteSuccess()) {
            DialogUtil.showInfoText(DialogUtil.message("message.bulk.allSent", result.sent()));
            return;
        }
        StringBuilder report = new StringBuilder(DialogUtil.message("message.bulk.sentCount", result.sent()));
        if (!result.unreadable().isEmpty()) {
            report.append("\n\n").append(DialogUtil.message("message.bulk.unreadable",
                    result.unreadable().size(), String.join(", ", result.unreadable())));
        }
        for (BulkMessageResult.Failure failure : result.refused()) {
            report.append("\n").append(failure.name()).append(": ").append(failure.reason());
        }
        DialogUtil.showInfoText(report.toString());
    }

    private void showSmsResult(SmsResult result) {
        if (result.success()) {
            markTemplateUsed();
            DialogUtil.showInfo("message.sms.sent");
            return;
        }
        DialogUtil.showInfoText(reasonText(result));
    }

    /** The provider's own words when this program does not know the code. */
    private String reasonText(SmsResult result) {
        return result.detail() == null || result.detail().isBlank()
                ? DialogUtil.message(result.code())
                : DialogUtil.message(result.code()) + " (" + result.detail() + ")";
    }

    @FXML
    private void onCopy() {
        if (WhatsAppLauncher.copyToClipboard(messageArea.getText())) {
            DialogUtil.showInfo("message.copied");
        } else {
            DialogUtil.showErrorMessage("message.copy.failed");
        }
    }

    private Optional<String> phoneNumber() {
        Optional<String> phone = PhoneNumbers.toWhatsApp(phoneField.getText());
        if (phone.isEmpty()) {
            DialogUtil.showErrorMessage("message.phone.unreadable");
        }
        return phone;
    }

    /** Counts the template as used so the templates screen can show what earns its place. */
    private void markTemplateUsed() {
        TemplateView chosen = templateComboBox.getValue();
        if (chosen != null) {
            templateService.markUsed(chosen.id());
        }
    }

    private static void hide(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    @FXML
    private void onClose(ActionEvent event) {
        ((Stage) ((Button) event.getSource()).getScene().getWindow()).close();
    }

    /** Message templates the user has written; empty until they write the first. */
    public List<TemplateView> templates() {
        return templateComboBox.getItems();
    }
}
