package com.electrician.tracker.service;

import com.electrician.tracker.dto.SessionUser;

/**
 * "Hazırlayan" and "Unvan" of a quote: free texts, not tied to a user
 * account (a deleted user never changes old quotes). A field left blank is
 * filled with the logged-in user's name (or title); if that is empty too, it
 * stays empty and the PDF shows only the "Hazırlayan" caption.
 */
public record QuotePreparer(String name, String title) {

    public static QuotePreparer resolve(String typedName, String typedTitle, SessionUser user) {
        String name = blankToNull(typedName);
        String title = blankToNull(typedTitle);
        if (name == null && user != null) {
            name = blankToNull(user.fullName());
        }
        if (title == null && user != null) {
            title = blankToNull(user.title());
        }
        return new QuotePreparer(name, title);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
