package com.electrician.tracker.ui.util;

import java.util.function.Consumer;

import com.electrician.tracker.config.FxmlViewLoader;
import com.electrician.tracker.config.FxmlViewLoader.LoadedView;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Component;

/**
 * Swaps the main window's content area. Lets any screen open another one
 * (optionally seeding its controller) without depending on MainController.
 * A screen the user may not open shows "Yetkiniz yok" and the current screen
 * stays; a screen with unsaved in-place edits asks before it is left.
 */
@Component
public class ContentNavigator {

    private final FxmlViewLoader fxmlViewLoader;
    private final ScreenAccess screenAccess;
    private final ReadOnlyStringWrapper currentView = new ReadOnlyStringWrapper();
    private StackPane contentArea;
    private Object currentController;

    public ContentNavigator(FxmlViewLoader fxmlViewLoader, ScreenAccess screenAccess) {
        this.fxmlViewLoader = fxmlViewLoader;
        this.screenAccess = screenAccess;
    }

    public void attach(StackPane area) {
        this.contentArea = area;
        this.currentController = null;
        currentView.set(null);
    }

    /** Forgets the shown screen and its data (logout / user switch). */
    public void clear() {
        if (contentArea != null) {
            contentArea.getChildren().clear();
        }
        contentArea = null;
        currentController = null;
        currentView.set(null);
    }

    /** FXML path of the screen on display, for highlighting its menu item. */
    public ReadOnlyStringProperty currentViewProperty() {
        return currentView.getReadOnlyProperty();
    }

    public boolean show(String fxmlPath) {
        return show(fxmlPath, controller -> {
        });
    }

    /** @return whether the screen was opened (false when locked or the user kept their edits) */
    public <T> boolean show(String fxmlPath, Consumer<T> beforeShow) {
        if (!screenAccess.canOpen(fxmlPath)) {
            ScreenAccess.showDenied();
            return false;
        }
        if (!confirmLeavingCurrent()) {
            return false;
        }
        LoadedView<T> view = fxmlViewLoader.loadWithController(fxmlPath);
        beforeShow.accept(view.controller());
        contentArea.getChildren().setAll(view.root());
        currentController = view.controller();
        currentView.set(fxmlPath);
        return true;
    }

    /** True when nothing unsaved would be lost, or the user agreed to drop it. */
    public boolean confirmLeavingCurrent() {
        if (currentController instanceof UnsavedChangesAware aware && aware.hasUnsavedChanges()) {
            return DialogUtil.confirm("unsaved.confirmLeave");
        }
        return true;
    }
}
