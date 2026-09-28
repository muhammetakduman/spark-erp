package com.electrician.tracker.ui.util;

/**
 * A screen with fields that are edited in place (not in a dialog). Before
 * the screen is left — another menu item, switching user — the user is asked
 * whether to drop what was not saved.
 */
public interface UnsavedChangesAware {

    boolean hasUnsavedChanges();
}
