package com.electrician.tracker.ui.util;

import java.util.function.Consumer;

import com.electrician.tracker.config.FxmlViewLoader;
import com.electrician.tracker.config.FxmlViewLoader.LoadedView;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Component;

/**
 * Swaps the main window's content area. Lets any screen open another one
 * (optionally seeding its controller) without depending on MainController.
 */
@Component
public class ContentNavigator {

    private final FxmlViewLoader fxmlViewLoader;
    private StackPane contentArea;

    public ContentNavigator(FxmlViewLoader fxmlViewLoader) {
        this.fxmlViewLoader = fxmlViewLoader;
    }

    public void attach(StackPane area) {
        this.contentArea = area;
    }

    public void show(String fxmlPath) {
        show(fxmlPath, controller -> {
        });
    }

    public <T> void show(String fxmlPath, Consumer<T> beforeShow) {
        LoadedView<T> view = fxmlViewLoader.loadWithController(fxmlPath);
        beforeShow.accept(view.controller());
        contentArea.getChildren().setAll(view.root());
    }
}
