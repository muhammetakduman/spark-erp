package com.electrician.tracker.service.messaging;

/**
 * How a message reaches the customer. WhatsApp opens the chat with the text
 * ready and the user presses send themselves, so it costs nothing and needs no
 * account; SMS leaves through the user's own provider account and is charged,
 * but reaches every phone and can go to a whole list at once.
 */
public enum MessageChannel {
    WHATSAPP,
    SMS
}
