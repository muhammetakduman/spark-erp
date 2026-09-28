package com.electrician.tracker.ui.util;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

/**
 * Runs an action once typing has paused, instead of on every key stroke
 * (e.g. filtering a list while searching).
 */
public final class Debouncer {

    public static final Duration TYPING_PAUSE = Duration.millis(250);

    private final PauseTransition pause;

    public Debouncer(Duration delay, Runnable action) {
        pause = new PauseTransition(delay);
        pause.setOnFinished(event -> action.run());
    }

    /** (Re)starts the wait; the action runs when nothing triggers it again within the delay. */
    public void trigger() {
        pause.playFromStart();
    }
}
