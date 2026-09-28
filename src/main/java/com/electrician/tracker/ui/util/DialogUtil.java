package com.electrician.tracker.ui.util;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.electrician.tracker.service.exception.LoginBlockedException;
import com.electrician.tracker.service.exception.ReferencedEntityException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * Small wrapper around JavaFX {@link Alert} so every screen shows errors and
 * confirmations the same way, with Turkish text resolved from the bundle.
 */
public final class DialogUtil {

    private static final String MESSAGES_BUNDLE = "messages_tr";

    private DialogUtil() {
    }

    /**
     * Shows a service/validation exception whose message is a resource
     * bundle key, translated to Turkish before display.
     */
    public static void showError(RuntimeException ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR, errorText(ex));
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /** Translated text of a service exception, with its arguments filled in. */
    public static String errorText(RuntimeException ex) {
        if (ex instanceof ReferencedEntityException referenced) {
            return message(referenced.getMessage(), referenced.getReferenceCount());
        }
        if (ex instanceof LoginBlockedException blocked) {
            return message(blocked.getMessage(), String.valueOf(blocked.getRemainingSeconds()));
        }
        return resolveMessage(ex.getMessage());
    }

    /**
     * Last-resort display for exceptions nobody caught (e.g. thrown inside a
     * button handler), so they are never silently swallowed.
     */
    public static void showUnexpected(Throwable failure) {
        Throwable root = failure;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String detail = root.getMessage() == null ? root.getClass().getSimpleName() : resolveMessage(root.getMessage());
        Alert alert = new Alert(Alert.AlertType.ERROR, message("error.unexpected", detail));
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static void showErrorMessage(String messageKey) {
        Alert alert = new Alert(Alert.AlertType.ERROR, resolveMessage(messageKey));
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static void showInfo(String messageKey) {
        showInfoText(resolveMessage(messageKey));
    }

    /** Information with already-resolved text (e.g. a message with arguments). */
    public static void showInfoText(String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, text);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static String message(String messageKey) {
        return resolveMessage(messageKey);
    }

    public static String message(String messageKey, Object... args) {
        return MessageFormat.format(resolveMessage(messageKey), args);
    }

    public static boolean confirm(String messageKey) {
        return confirmText(resolveMessage(messageKey));
    }

    public static boolean confirm(String messageKey, Object... args) {
        return confirmText(message(messageKey, args));
    }

    /** Yes/no question with already-resolved text; true when the user answers yes. */
    public static boolean confirmText(String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        return alert.showAndWait().filter(button -> button == ButtonType.YES).isPresent();
    }

    private static String resolveMessage(String messageKey) {
        try {
            return ResourceBundle.getBundle(MESSAGES_BUNDLE).getString(messageKey);
        } catch (MissingResourceException e) {
            return messageKey;
        }
    }
}
