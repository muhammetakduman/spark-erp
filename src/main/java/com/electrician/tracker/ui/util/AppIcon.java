package com.electrician.tracker.ui.util;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * Every icon of the application, one per meaning, so the same action shows
 * the same icon everywhere (delete is always the bin, save always the disk).
 * {@link #tone} is an extra style class for the few coloured icons (delete
 * and PDF red, Excel green, locked grey); colours themselves live in CSS.
 * FXML buttons get their icon through the style class {@code action-<name>},
 * e.g. {@code action-save} (see {@link ButtonIcons}).
 */
public enum AppIcon {
    APP(FontAwesomeSolid.BOLT, null),
    HOME(FontAwesomeSolid.TACHOMETER_ALT, null),
    SITES(FontAwesomeSolid.BUILDING, null),
    SERVICES(FontAwesomeSolid.TOOLBOX, null),
    QUOTES(FontAwesomeSolid.FILE_INVOICE, null),
    PRODUCTS(FontAwesomeSolid.BOX, null),
    CUSTOMERS(FontAwesomeSolid.USERS, null),
    EMPLOYEES(FontAwesomeSolid.ID_BADGE, null),
    ATTENDANCE(FontAwesomeSolid.USER_CLOCK, null),
    DAILY_PLAN(FontAwesomeSolid.CALENDAR_CHECK, null),
    MONTHLY_REPORT(FontAwesomeSolid.CHART_LINE, null),
    USERS(FontAwesomeSolid.USERS_COG, null),
    SETTINGS(FontAwesomeSolid.COG, null),
    ABOUT(FontAwesomeSolid.INFO_CIRCLE, null),
    ADD(FontAwesomeSolid.PLUS, null),
    EDIT(FontAwesomeSolid.EDIT, null),
    DELETE(FontAwesomeSolid.TRASH_ALT, "icon-danger"),
    SAVE(FontAwesomeSolid.SAVE, null),
    PDF(FontAwesomeSolid.FILE_PDF, "icon-danger"),
    EXCEL(FontAwesomeSolid.FILE_EXCEL, "icon-success"),
    LOCKED(FontAwesomeSolid.LOCK, "icon-locked"),
    COPY(FontAwesomeSolid.COPY, null),
    OPEN(FontAwesomeSolid.FOLDER_OPEN, null),
    MORE(FontAwesomeSolid.ELLIPSIS_H, null),
    MOVE_UP(FontAwesomeSolid.ARROW_UP, null),
    MOVE_DOWN(FontAwesomeSolid.ARROW_DOWN, null),
    DONE(FontAwesomeSolid.CHECK, "icon-on-color"),
    NOT_DONE(FontAwesomeSolid.TIMES, "icon-on-color"),
    PREVIOUS(FontAwesomeSolid.CHEVRON_LEFT, null),
    NEXT(FontAwesomeSolid.CHEVRON_RIGHT, null),
    TODAY(FontAwesomeSolid.CALENDAR_DAY, null),
    PLAN_DAY(FontAwesomeSolid.CALENDAR_PLUS, null),
    PRINT(FontAwesomeSolid.PRINT, null),
    TEMPLATE(FontAwesomeSolid.CLIPBOARD_LIST, null),
    FROM_TEMPLATE(FontAwesomeSolid.FILE_IMPORT, null),
    PAYMENT(FontAwesomeSolid.HAND_HOLDING_USD, null),
    CURRENCY(FontAwesomeSolid.COINS, null),
    IMAGE(FontAwesomeSolid.IMAGE, null),
    REMOVE(FontAwesomeSolid.TIMES, null),
    USER(FontAwesomeSolid.USER, null),
    PASSWORD(FontAwesomeSolid.KEY, null),
    PASSWORD_FIELD(FontAwesomeSolid.LOCK, null),
    SHOW_PASSWORD(FontAwesomeSolid.EYE, null),
    SWITCH_USER(FontAwesomeSolid.EXCHANGE_ALT, null),
    LOG_OUT(FontAwesomeSolid.SIGN_OUT_ALT, null),
    BACKUP(FontAwesomeSolid.DATABASE, null),
    RESTORE(FontAwesomeSolid.UNDO, null),
    WARNING(FontAwesomeSolid.EXCLAMATION_TRIANGLE, "icon-warning"),
    INFO(FontAwesomeSolid.INFO_CIRCLE, null),
    PENDING(FontAwesomeSolid.INBOX, null),
    WEEK(FontAwesomeSolid.CALENDAR_WEEK, null),
    CLOCK(FontAwesomeSolid.CLOCK, null),
    PLACE(FontAwesomeSolid.MAP_MARKER_ALT, null),
    PHONE(FontAwesomeSolid.PHONE, null),
    SEARCH(FontAwesomeSolid.SEARCH, null),
    PALETTE(FontAwesomeSolid.PALETTE, null),
    FOLDER(FontAwesomeSolid.FOLDER_OPEN, null),
    CANCEL(FontAwesomeSolid.BAN, null);

    private final Ikon ikon;
    private final String tone;

    AppIcon(Ikon ikon, String tone) {
        this.ikon = ikon;
        this.tone = tone;
    }

    public Ikon ikon() {
        return ikon;
    }

    /** Extra style class (e.g. {@code icon-danger}); {@code null} for the plain one-colour icon. */
    public String tone() {
        return tone;
    }
}
