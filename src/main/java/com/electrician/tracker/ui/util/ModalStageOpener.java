package com.electrician.tracker.ui.util;

import java.util.function.Consumer;

import com.electrician.tracker.config.FxmlViewLoader;
import com.electrician.tracker.config.FxmlViewLoader.LoadedView;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/**
 * Opens an FXML view as a modal dialog window and returns its controller so
 * the caller can seed it with data or read a result after it closes. Every
 * dialog gets the same header: its icon and title side by side.
 */
@Component
public class ModalStageOpener {

    private final FxmlViewLoader fxmlViewLoader;

    public ModalStageOpener(FxmlViewLoader fxmlViewLoader) {
        this.fxmlViewLoader = fxmlViewLoader;
    }

    public <T> T openAndWait(String fxmlPath, String titleKey, Window owner) {
        return openAndWait(fxmlPath, titleKey, owner, controller -> {
        });
    }

    public <T> T openAndWait(String fxmlPath, String titleKey, Window owner, Consumer<T> beforeShow) {
        LoadedView<T> view = fxmlViewLoader.loadWithController(fxmlPath);
        beforeShow.accept(view.controller());
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(owner == null ? Modality.APPLICATION_MODAL : Modality.WINDOW_MODAL);
        String title = DialogUtil.message(titleKey);
        stage.setTitle(title);
        BorderPane frame = new BorderPane(view.root());
        frame.setTop(header(fxmlPath, title));
        frame.getStyleClass().add("dialog-frame");
        Scene scene = new Scene(frame);
        Stylesheets.apply(scene);
        stage.setScene(scene);
        stage.setOnShown(event -> fitOnScreen(stage));
        stage.showAndWait();
        return view.controller();
    }

    /** A dialog taller or wider than the screen is shrunk to it (its content scrolls) and centred. */
    private static void fitOnScreen(Stage stage) {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        if (stage.getHeight() > screen.getHeight()) {
            stage.setHeight(screen.getHeight());
        }
        if (stage.getWidth() > screen.getWidth()) {
            stage.setWidth(screen.getWidth());
        }
        stage.centerOnScreen();
        if (stage.getY() < screen.getMinY()) {
            stage.setY(screen.getMinY());
        }
    }

    private static Label header(String fxmlPath, String title) {
        Label header = new Label(title, Icons.of(ViewPaths.dialogIcon(fxmlPath), IconSize.TITLE));
        header.getStyleClass().add("dialog-header");
        header.setMaxWidth(Double.MAX_VALUE);
        return header;
    }
}
