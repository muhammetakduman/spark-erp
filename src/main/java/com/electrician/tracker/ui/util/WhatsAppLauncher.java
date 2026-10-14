package com.electrician.tracker.ui.util;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * Opens WhatsApp with the message already typed, so the user only presses
 * send. Nothing is sent on their behalf and no account is needed: this is the
 * ordinary click-to-chat link every WhatsApp client understands.
 *
 * <p>Two addresses are tried in turn. The {@code whatsapp:} one hands the chat
 * straight to the desktop application; the {@code https://wa.me/} one goes
 * through the browser, which then offers to open WhatsApp. When neither opens
 * — no WhatsApp installed, no browser, or no connection — the text goes to the
 * clipboard instead, so the work of composing it is never lost.
 */
public final class WhatsAppLauncher {

    private static final String APP_URL = "whatsapp://send?phone=%s&text=%s";
    private static final String WEB_URL = "https://wa.me/%s?text=%s";

    private WhatsAppLauncher() {
    }

    /** What happened, so the caller can tell the user. */
    public enum Outcome {
        OPENED,
        COPIED_TO_CLIPBOARD,
        FAILED
    }

    public static Outcome open(String internationalPhone, String message) {
        String encoded = URLEncoder.encode(message == null ? "" : message, StandardCharsets.UTF_8);
        if (browse(APP_URL.formatted(internationalPhone, encoded))
                || browse(WEB_URL.formatted(internationalPhone, encoded))) {
            return Outcome.OPENED;
        }
        return copyToClipboard(message) ? Outcome.COPIED_TO_CLIPBOARD : Outcome.FAILED;
    }

    /** Offered beside the send button for whoever would rather paste it themselves. */
    public static boolean copyToClipboard(String message) {
        try {
            ClipboardContent content = new ClipboardContent();
            content.putString(message == null ? "" : message);
            return Clipboard.getSystemClipboard().setContent(content);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Desktop support differs by system and by protocol, and an unregistered
     * scheme throws rather than returning false, so every failure here simply
     * means "try the next address".
     */
    private static boolean browse(String url) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                return false;
            }
            Desktop.getDesktop().browse(URI.create(url));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
