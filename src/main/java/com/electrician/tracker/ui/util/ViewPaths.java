package com.electrician.tracker.ui.util;

import java.util.Map;

/** FXML locations of every screen and dialog, so no path is written twice. */
public final class ViewPaths {

    public static final String MAIN = "/fxml/main.fxml";
    public static final String HOME = "/fxml/home_screen.fxml";
    public static final String SITES = "/fxml/site_list.fxml";
    public static final String SERVICES = "/fxml/service_list.fxml";
    public static final String DAILY_PLAN = "/fxml/daily_plan.fxml";
    public static final String QUOTES = "/fxml/quote_list.fxml";
    public static final String MONTHLY_REPORT = "/fxml/monthly_report.fxml";
    public static final String CUSTOMERS = "/fxml/customer_list.fxml";
    public static final String EMPLOYEES = "/fxml/employee_list.fxml";
    public static final String EMPLOYEE_ATTENDANCE = "/fxml/employee_attendance.fxml";
    public static final String PRODUCTS = "/fxml/product_list.fxml";
    public static final String USERS = "/fxml/users_screen.fxml";
    public static final String SETTINGS = "/fxml/settings_screen.fxml";

    public static final String LICENSE_DIALOG = "/fxml/license_dialog.fxml";
    public static final String LOGIN_DIALOG = "/fxml/login_dialog.fxml";
    public static final String SETUP_DIALOG = "/fxml/initial_setup.fxml";
    public static final String USER_FORM = "/fxml/user_form.fxml";
    public static final String PASSWORD_DIALOG = "/fxml/password_dialog.fxml";
    public static final String JOB_FORM = "/fxml/job_form.fxml";
    public static final String SERVICE_FORM = "/fxml/service_form.fxml";
    public static final String MATERIAL_DIALOG = "/fxml/material_dialog.fxml";
    public static final String ATTENDANCE_DIALOG = "/fxml/attendance_entry.fxml";
    public static final String PAYMENT_DIALOG = "/fxml/payment_dialog.fxml";
    public static final String CUSTOMER_FORM = "/fxml/customer_form.fxml";
    public static final String EMPLOYEE_FORM = "/fxml/employee_form.fxml";
    public static final String PRODUCT_FORM = "/fxml/product_form.fxml";
    public static final String QUOTE_FORM = "/fxml/quote_form.fxml";
    public static final String QUOTE_ITEM_DIALOG = "/fxml/quote_item_dialog.fxml";
    public static final String DAILY_JOB_FORM = "/fxml/daily_job_form.fxml";
    public static final String NOT_VISITED_DIALOG = "/fxml/not_visited_dialog.fxml";
    public static final String TEMPLATE_FORM = "/fxml/template_form.fxml";

    /** The icon next to each dialog's title. */
    private static final Map<String, AppIcon> DIALOG_ICONS = Map.ofEntries(
            Map.entry(LICENSE_DIALOG, AppIcon.APP),
            Map.entry(USER_FORM, AppIcon.USER),
            Map.entry(PASSWORD_DIALOG, AppIcon.PASSWORD),
            Map.entry(JOB_FORM, AppIcon.SITES),
            Map.entry(SERVICE_FORM, AppIcon.SERVICES),
            Map.entry(MATERIAL_DIALOG, AppIcon.PRODUCTS),
            Map.entry(ATTENDANCE_DIALOG, AppIcon.ATTENDANCE),
            Map.entry(PAYMENT_DIALOG, AppIcon.PAYMENT),
            Map.entry(CUSTOMER_FORM, AppIcon.CUSTOMERS),
            Map.entry(EMPLOYEE_FORM, AppIcon.EMPLOYEES),
            Map.entry(PRODUCT_FORM, AppIcon.PRODUCTS),
            Map.entry(QUOTE_FORM, AppIcon.QUOTES),
            Map.entry(QUOTE_ITEM_DIALOG, AppIcon.QUOTES),
            Map.entry(DAILY_JOB_FORM, AppIcon.DAILY_PLAN),
            Map.entry(NOT_VISITED_DIALOG, AppIcon.DAILY_PLAN),
            Map.entry(TEMPLATE_FORM, AppIcon.TEMPLATE));

    private ViewPaths() {
    }

    public static AppIcon dialogIcon(String fxmlPath) {
        return DIALOG_ICONS.getOrDefault(fxmlPath, AppIcon.APP);
    }
}
