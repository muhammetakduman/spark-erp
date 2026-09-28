package com.electrician.tracker.ui.controller;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.electrician.tracker.dto.SessionUser;
import com.electrician.tracker.domain.UserRole;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.ui.util.AppIcon;
import com.electrician.tracker.ui.util.AppSignature;
import com.electrician.tracker.ui.util.ContentNavigator;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.IconSize;
import com.electrician.tracker.ui.util.Icons;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.PendingJobsBadge;
import com.electrician.tracker.ui.util.ScreenAccess;
import com.electrician.tracker.ui.util.StageManager;
import com.electrician.tracker.ui.util.ViewPaths;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The main window, built fresh for every login: header with the user menu
 * (avatar, name, role; change password, switch user, exit), the side menu
 * with an icon per screen and the content area. Screens the role may not
 * open stay in the menu, greyed out with a lock. "İş Takip" carries the
 * number of forgotten daily jobs.
 */
@Component
@Scope("prototype")
public class MainController {

    private static final String LOCKED_STYLE = "menu-button-locked";
    private static final String ACTIVE_STYLE = "menu-button-active";
    private static final String ADMIN_BADGE_STYLE = "role-badge-admin";
    private static final double USER_BOX_SPACING = 8;
    private static final double NAME_BOX_SPACING = 1;

    private final ContentNavigator contentNavigator;
    private final ModalStageOpener modalStageOpener;
    private final AppSignature appSignature;
    private final AccessControl accessControl;
    private final ScreenAccess screenAccess;
    private final StageManager stageManager;
    private final PendingJobsBadge pendingJobsBadge;

    @FXML
    private StackPane contentArea;
    @FXML
    private Label appTitleLabel;
    @FXML
    private MenuButton userMenuButton;
    @FXML
    private MenuItem switchUserItem;
    @FXML
    private Button homeButton;
    @FXML
    private Button sitesButton;
    @FXML
    private Button servicesButton;
    @FXML
    private Button dailyPlanButton;
    @FXML
    private Label pendingBadge;
    @FXML
    private Button quotesButton;
    @FXML
    private Button customersButton;
    @FXML
    private Button productsButton;
    @FXML
    private Button employeesButton;
    @FXML
    private Button attendanceButton;
    @FXML
    private Button monthlyReportButton;
    @FXML
    private Button usersButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button aboutButton;
    @FXML
    private Label signatureVersionLabel;
    @FXML
    private Label signatureNoticeLabel;
    @FXML
    private Label signatureCopyrightLabel;

    private final Map<String, Button> menuByView = new LinkedHashMap<>();

    public MainController(ContentNavigator contentNavigator, ModalStageOpener modalStageOpener,
            AppSignature appSignature, AccessControl accessControl, ScreenAccess screenAccess,
            StageManager stageManager, PendingJobsBadge pendingJobsBadge) {
        this.contentNavigator = contentNavigator;
        this.modalStageOpener = modalStageOpener;
        this.appSignature = appSignature;
        this.accessControl = accessControl;
        this.screenAccess = screenAccess;
        this.stageManager = stageManager;
        this.pendingJobsBadge = pendingJobsBadge;
    }

    @FXML
    private void initialize() {
        appTitleLabel.setGraphic(Icons.of(AppIcon.APP, IconSize.TITLE));
        switchUserItem.setAccelerator(StageManager.SWITCH_USER_KEYS);
        accessControl.currentUser().ifPresent(this::showUser);
        setUpMenu();
        signatureVersionLabel.setText(appSignature.versionLine());
        signatureNoticeLabel.setText(appSignature.noticeLine());
        signatureCopyrightLabel.setText(appSignature.copyrightLine());
        pendingBadge.textProperty().bind(pendingJobsBadge.countProperty().asString());
        pendingBadge.visibleProperty().bind(pendingJobsBadge.countProperty().greaterThan(0));
        contentNavigator.attach(contentArea);
        contentNavigator.currentViewProperty().addListener((obs, old, view) -> highlight(view));
        contentNavigator.show(screenAccess.startScreen());
    }

    /** Avatar circle with initials, name and role badge. */
    private void showUser(SessionUser user) {
        Label avatar = new Label(initials(user.fullName()));
        avatar.getStyleClass().add("avatar");
        Label name = new Label(user.fullName());
        name.getStyleClass().add("user-menu-name");
        name.setMinWidth(Region.USE_PREF_SIZE);
        Label role = new Label(EnumLabels.label(user.role()));
        role.getStyleClass().add("role-badge");
        if (user.role() == UserRole.ADMIN) {
            role.getStyleClass().add(ADMIN_BADGE_STYLE);
        }
        VBox nameBox = new VBox(NAME_BOX_SPACING, name, role);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        HBox box = new HBox(USER_BOX_SPACING, avatar, nameBox);
        box.setAlignment(Pos.CENTER_LEFT);
        userMenuButton.setGraphic(box);
    }

    static String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase(Locale.forLanguageTag("tr-TR"));
    }

    private void setUpMenu() {
        register(homeButton, ViewPaths.HOME, AppIcon.HOME);
        register(sitesButton, ViewPaths.SITES, AppIcon.SITES);
        register(servicesButton, ViewPaths.SERVICES, AppIcon.SERVICES);
        register(dailyPlanButton, ViewPaths.DAILY_PLAN, AppIcon.DAILY_PLAN);
        register(quotesButton, ViewPaths.QUOTES, AppIcon.QUOTES);
        register(customersButton, ViewPaths.CUSTOMERS, AppIcon.CUSTOMERS);
        register(productsButton, ViewPaths.PRODUCTS, AppIcon.PRODUCTS);
        register(employeesButton, ViewPaths.EMPLOYEES, AppIcon.EMPLOYEES);
        register(attendanceButton, ViewPaths.EMPLOYEE_ATTENDANCE, AppIcon.ATTENDANCE);
        register(monthlyReportButton, ViewPaths.MONTHLY_REPORT, AppIcon.MONTHLY_REPORT);
        register(usersButton, ViewPaths.USERS, AppIcon.USERS);
        register(settingsButton, ViewPaths.SETTINGS, AppIcon.SETTINGS);
        aboutButton.setGraphic(Icons.of(AppIcon.ABOUT, IconSize.MENU));
    }

    /** Menu icon, or a lock and faded look when the role may not open the screen (it stays clickable). */
    private void register(Button button, String view, AppIcon icon) {
        menuByView.put(view, button);
        boolean locked = !screenAccess.canOpen(view);
        button.setGraphic(Icons.of(locked ? AppIcon.LOCKED : icon, IconSize.MENU));
        if (locked) {
            button.getStyleClass().add(LOCKED_STYLE);
        }
    }

    private void highlight(String view) {
        menuByView.forEach((path, button) -> {
            button.getStyleClass().remove(ACTIVE_STYLE);
            if (path.equals(view)) {
                button.getStyleClass().add(ACTIVE_STYLE);
            }
        });
        pendingJobsBadge.refresh();
    }

    @FXML
    private void showAbout() {
        modalStageOpener.<LicenseDialogController>openAndWait(ViewPaths.LICENSE_DIALOG, "menu.about",
                contentArea.getScene().getWindow(), LicenseDialogController::showAsAbout);
    }

    @FXML
    private void onChangePassword() {
        accessControl.currentUser().ifPresent(user -> modalStageOpener.<PasswordDialogController>openAndWait(
                ViewPaths.PASSWORD_DIALOG, "user.dialog.password", contentArea.getScene().getWindow(),
                controller -> controller.forUser(user.id(), user.fullName())));
    }

    @FXML
    private void onSwitchUser() {
        stageManager.switchUser();
    }

    @FXML
    private void onExit() {
        stageManager.exit();
    }

    @FXML
    private void showHome() {
        contentNavigator.show(ViewPaths.HOME);
    }

    @FXML
    private void showSites() {
        contentNavigator.show(ViewPaths.SITES);
    }

    @FXML
    private void showServices() {
        contentNavigator.show(ViewPaths.SERVICES);
    }

    @FXML
    private void showDailyPlan() {
        contentNavigator.show(ViewPaths.DAILY_PLAN);
    }

    @FXML
    private void showQuotes() {
        contentNavigator.show(ViewPaths.QUOTES);
    }

    @FXML
    private void showMonthlyReport() {
        contentNavigator.show(ViewPaths.MONTHLY_REPORT);
    }

    @FXML
    private void showUsers() {
        contentNavigator.show(ViewPaths.USERS);
    }

    @FXML
    private void showCustomers() {
        contentNavigator.show(ViewPaths.CUSTOMERS);
    }

    @FXML
    private void showEmployees() {
        contentNavigator.show(ViewPaths.EMPLOYEES);
    }

    @FXML
    private void showEmployeeAttendance() {
        contentNavigator.show(ViewPaths.EMPLOYEE_ATTENDANCE);
    }

    @FXML
    private void showProducts() {
        contentNavigator.show(ViewPaths.PRODUCTS);
    }

    @FXML
    private void showSettings() {
        contentNavigator.show(ViewPaths.SETTINGS);
    }
}
