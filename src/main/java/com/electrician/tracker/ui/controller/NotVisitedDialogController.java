package com.electrician.tracker.ui.controller;

import java.util.Optional;

import com.electrician.tracker.dto.DailyJobCard;
import com.electrician.tracker.ui.util.DialogUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Gidilmedi": an optional reason and "Ertesi güne aktar" (ticked by
 * default). The plan screen does the rest with the answer.
 */
@Component
@Scope("prototype")
public class NotVisitedDialogController {

    @FXML
    private Label jobLabel;
    @FXML
    private TextField reasonField;
    @FXML
    private CheckBox moveCheckBox;
    @FXML
    private Button saveButton;

    private Decision decision;

    public void forJob(DailyJobCard card) {
        jobLabel.setText(DialogUtil.message("dailyJob.notVisited.job", card.title()));
    }

    /** The answer, or empty when the dialog was cancelled. */
    public Optional<Decision> getDecision() {
        return Optional.ofNullable(decision);
    }

    @FXML
    private void onSave() {
        decision = new Decision(reasonField.getText(), moveCheckBox.isSelected());
        close();
    }

    @FXML
    private void onCancel() {
        decision = null;
        close();
    }

    private void close() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }

    /** Reason (may be blank) and whether the job moves to the next day. */
    public record Decision(String reason, boolean moveToNextDay) {
    }
}
