package com.electrician.tracker.ui.util;

import java.util.Set;

import com.electrician.tracker.service.AccessControl;
import org.springframework.stereotype.Component;

/**
 * Which main screens the logged-in user may open. A MANAGER sees the locked
 * ones in the menu (greyed, with a lock) but gets "Yetkiniz yok" instead of
 * the screen and stays where they are. The services behind these screens
 * check the role again, so this is only the friendly front door.
 */
@Component
public class ScreenAccess {

    private static final Set<String> ADMIN_ONLY = Set.of(ViewPaths.HOME, ViewPaths.SITES, ViewPaths.MONTHLY_REPORT,
            ViewPaths.USERS);


    private final AccessControl accessControl;

    public ScreenAccess(AccessControl accessControl) {
        this.accessControl = accessControl;
    }

    public boolean canOpen(String fxmlPath) {
        return !ADMIN_ONLY.contains(fxmlPath) || accessControl.isAdmin();
    }

    /** Where a user lands after logging in: the dashboard for an ADMIN, services for a MANAGER. */
    public String startScreen() {
        return accessControl.isAdmin() ? ViewPaths.HOME : ViewPaths.SERVICES;
    }

    /** "Yetkiniz yok" — shown instead of opening a locked screen. */
    public static void showDenied() {
        SparkDialog.warning(DialogUtil.message("access.denied.title"), DialogUtil.message("access.denied.text"));
    }
}
