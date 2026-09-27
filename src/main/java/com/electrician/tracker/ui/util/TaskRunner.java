package com.electrician.tracker.ui.util;

import java.util.function.Supplier;

import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import org.springframework.stereotype.Component;

/**
 * Runs database work off the JavaFX Application Thread and toggles a
 * loading indicator, so screens stay responsive per the project's "long
 * work runs in background Tasks with a loading indicator" rule.
 */
@Component
public class TaskRunner {

    public <T> void run(Supplier<T> work, java.util.function.Consumer<T> onSuccess, ProgressIndicator indicator,
            Node content) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return work.get();
            }
        };
        indicator.visibleProperty().bind(task.runningProperty());
        content.disableProperty().bind(task.runningProperty());
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> {
            Throwable failure = task.getException();
            if (failure instanceof RuntimeException runtimeException) {
                DialogUtil.showError(runtimeException);
            } else {
                DialogUtil.showError(new RuntimeException(failure));
            }
        });
        Thread thread = new Thread(task, "db-task");
        thread.setDaemon(true);
        thread.start();
    }
}
