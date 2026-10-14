package com.electrician.tracker.dto.messaging;

import java.util.Map;

/**
 * Who a message is for and what a template may put into it: the name to show,
 * the phone as it was typed into the customer record, and the already
 * formatted values behind "{musteri}", "{bakiye}" and the rest.
 *
 * <p>The values arrive formatted because how money and dates read belongs to
 * the screen, not to the service that fills the template.
 */
public record MessageTarget(String name, String rawPhone, Map<String, String> values) {

    public static MessageTarget of(String name, String rawPhone, Map<String, String> values) {
        return new MessageTarget(name, rawPhone, values == null ? Map.of() : Map.copyOf(values));
    }
}
