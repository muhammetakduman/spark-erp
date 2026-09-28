package com.electrician.tracker.ui.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.DailyJobPriority;
import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.domain.TemplateType;
import com.electrician.tracker.dto.DailyJobDraft;
import com.electrician.tracker.dto.EmployeeBooking;
import com.electrician.tracker.dto.JobOption;
import com.electrician.tracker.dto.TemplateView;
import com.electrician.tracker.service.CustomerService;
import com.electrician.tracker.service.DailyJobService;
import com.electrician.tracker.service.EmployeeService;
import com.electrician.tracker.service.TemplateService;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.CustomerNameField;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.SuggestionPicker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Adds or changes a daily job: day, optional time, title (typed or picked
 * from the job title templates), optional link to an active site or service
 * (fills title, customer, address and phone), customer, address, phone, who
 * goes, urgent and a note. When someone is already on another job that day
 * the form asks before saving.
 */
@Component
@Scope("prototype")
public class DailyJobFormController {

    private final DailyJobService dailyJobService;
    private final CustomerService customerService;
    private final EmployeeService employeeService;
    private final TemplateService templateService;

    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField timeField;
    @FXML
    private CheckBox urgentCheckBox;
    @FXML
    private ComboBox<JobOption> jobComboBox;
    @FXML
    private ComboBox<String> titleComboBox;
    @FXML
    private ComboBox<String> customerComboBox;
    @FXML
    private TextField addressField;
    @FXML
    private TextField phoneField;
    @FXML
    private FlowPane teamPane;
    @FXML
    private TextArea noteArea;
    @FXML
    private Button saveButton;

    private final List<TeamChoice> team = new ArrayList<>();
    private SuggestionPicker titlePicker;
    private CustomerNameField customerField;
    private Long editingId;
    private boolean saved;

    public DailyJobFormController(DailyJobService dailyJobService, CustomerService customerService,
            EmployeeService employeeService, TemplateService templateService) {
        this.dailyJobService = dailyJobService;
        this.customerService = customerService;
        this.employeeService = employeeService;
        this.templateService = templateService;
    }

    @FXML
    private void initialize() {
        datePicker.setValue(dailyJobService.today());
        titlePicker = new SuggestionPicker(titleComboBox, templateService.findByType(TemplateType.JOB_DESCRIPTION)
                .stream().map(TemplateView::content).toList());
        customerField = new CustomerNameField(customerComboBox, customerService.findAll());
        customerField.setOnCustomerChosen(this::fillFromCustomer);
        jobComboBox.getItems().setAll(dailyJobService.linkableJobs());
        jobComboBox.valueProperty().addListener((obs, old, option) -> fillFromJob(option));
        for (Employee employee : employeeService.findAllActive()) {
            CheckBox box = new CheckBox(employee.getName());
            team.add(new TeamChoice(employee.getId(), box));
            teamPane.getChildren().add(box);
        }
        if (team.isEmpty()) {
            Label none = new Label(DialogUtil.message("dailyJob.field.team.empty"));
            none.getStyleClass().add("muted-text");
            teamPane.getChildren().add(none);
        }
    }

    public void startForDate(LocalDate date) {
        if (date != null) {
            datePicker.setValue(date);
        }
    }

    /** "Bu şantiyeye gün planla": already linked to the site. */
    public void startForJob(Long jobId) {
        jobComboBox.getItems().stream().filter(option -> option.jobId().equals(jobId)).findFirst()
                .ifPresent(jobComboBox::setValue);
    }

    public void editExisting(Long id) {
        this.editingId = id;
        DailyJobDraft draft = dailyJobService.findDraft(id);
        datePicker.setValue(draft.date());
        timeField.setText(draft.timeOfDay());
        urgentCheckBox.setSelected(draft.priority() == DailyJobPriority.URGENT);
        jobComboBox.getItems().stream().filter(option -> option.jobId().equals(draft.jobId())).findFirst()
                .ifPresent(jobComboBox::setValue);
        titlePicker.select(draft.title());
        customerField.showName(draft.customerName());
        addressField.setText(draft.address());
        phoneField.setText(draft.phone());
        noteArea.setText(draft.note());
        team.forEach(choice -> choice.box().setSelected(draft.employeeIds().contains(choice.employeeId())));
    }

    public boolean isSaved() {
        return saved;
    }

    private void fillFromJob(JobOption option) {
        if (option == null) {
            return;
        }
        if (titlePicker.typedText().isEmpty()) {
            titlePicker.select(option.title());
        }
        customerField.showName(option.customerName());
        addressField.setText(option.address());
        phoneField.setText(option.phone());
    }

    private void fillFromCustomer(Customer customer) {
        addressField.setText(customer.getAddress());
        phoneField.setText(customer.getPhone());
    }

    @FXML
    private void onClearJob() {
        jobComboBox.setValue(null);
    }

    @FXML
    private void onSave() {
        DailyJobDraft draft = buildDraft();
        try {
            if (!confirmBookings(dailyJobService.findOtherBookings(editingId, draft))) {
                return;
            }
            dailyJobService.save(editingId, draft);
            saved = true;
            close();
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    /** "Mustafa Akbül 29.09.2026 tarihinde 2 işe daha yazılı. Yine de eklensin mi?" */
    private static boolean confirmBookings(List<EmployeeBooking> bookings) {
        if (bookings.isEmpty()) {
            return true;
        }
        String lines = bookings.stream()
                .map(booking -> DialogUtil.message("dailyJob.booking.line", booking.employeeName(),
                        Bicimlendirici.date(booking.date()), booking.otherJobCount()))
                .collect(Collectors.joining("\n"));
        return DialogUtil.confirmText(lines + "\n" + DialogUtil.message("dailyJob.booking.question"));
    }

    private DailyJobDraft buildDraft() {
        Customer customer = customerField.matchedCustomer();
        JobOption job = jobComboBox.getValue();
        List<Long> employeeIds = team.stream().filter(choice -> choice.box().isSelected())
                .map(TeamChoice::employeeId).toList();
        return new DailyJobDraft(datePicker.getValue(), timeField.getText(), titlePicker.typedText(),
                customer == null ? null : customer.getId(), customerField.typedName(), addressField.getText(),
                phoneField.getText(), employeeIds,
                urgentCheckBox.isSelected() ? DailyJobPriority.URGENT : DailyJobPriority.NORMAL,
                noteArea.getText(), job == null ? null : job.jobId());
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }

    private record TeamChoice(Long employeeId, CheckBox box) {
        TeamChoice {
            Objects.requireNonNull(employeeId);
        }
    }
}
