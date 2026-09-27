package com.electrician.tracker.ui.util;

/**
 * Resolves the Turkish display label for a domain enum constant from the
 * message bundle, using the key pattern {@code <SimpleClassName>.<NAME>}.
 */
public final class EnumLabels {

    private EnumLabels() {
    }

    public static String label(Enum<?> value) {
        if (value == null) {
            return "";
        }
        return DialogUtil.message(value.getClass().getSimpleName() + "." + value.name());
    }
}
