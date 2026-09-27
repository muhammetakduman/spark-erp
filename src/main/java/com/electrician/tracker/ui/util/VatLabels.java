package com.electrician.tracker.ui.util;

/**
 * Turkish display text for a VAT setting: "%20 dahil", "%20 hariç" or "—".
 */
public final class VatLabels {

    private VatLabels() {
    }

    public static String describe(Integer rate, Boolean included) {
        if (rate == null) {
            return DialogUtil.message("vat.label.none");
        }
        String key = Boolean.TRUE.equals(included) ? "vat.label.included" : "vat.label.excluded";
        return DialogUtil.message(key, String.valueOf(rate));
    }

    public static String rateLine(int rate) {
        return DialogUtil.message("vat.summary.rateLine", String.valueOf(rate));
    }
}
