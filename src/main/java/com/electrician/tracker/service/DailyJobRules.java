package com.electrician.tracker.service;

import java.util.regex.Pattern;

import com.electrician.tracker.dto.DailyJobDraft;
import com.electrician.tracker.service.exception.ValidationException;

/** Validation of a daily job before it is saved. */
final class DailyJobRules {

    /** "9:00", "09:00" … "23:59". */
    private static final Pattern TIME = Pattern.compile("([01]?\\d|2[0-3]):[0-5]\\d");
    private static final int PADDED_TIME_LENGTH = 5;

    private DailyJobRules() {
    }

    static void validate(DailyJobDraft draft) {
        if (draft.date() == null) {
            throw new ValidationException("error.dailyJob.date.required");
        }
        if (draft.title() == null || draft.title().isBlank()) {
            throw new ValidationException("error.dailyJob.title.required");
        }
        String time = draft.timeOfDay();
        if (time != null && !time.isBlank() && !TIME.matcher(time.trim()).matches()) {
            throw new ValidationException("error.dailyJob.time.invalid");
        }
    }

    /** "9:00" → "09:00" (so times sort as text); blank → {@code null}. */
    static String normalizeTime(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }
        String trimmed = time.trim();
        return trimmed.length() < PADDED_TIME_LENGTH ? "0" + trimmed : trimmed;
    }
}
