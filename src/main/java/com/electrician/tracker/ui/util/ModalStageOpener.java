package com.electrician.tracker.ui.util;

import java.util.function.Consumer;

import com.electrician.tracker.config.FxmlViewLoader;
import com.electrician.tracker.config.FxmlViewLoader.LoadedView;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/**
 * Opens an FXML view as a modal dialog window and returns its controller so
 * the caller can seed it with data or read a result after it closes.
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
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(DialogUtil.message(titleKey));
        Scene scene = new Scene(view.root());
        Stylesheets.apply(scene);
        stage.setScene(scene);
        stage.showAndWait();
        return view.controller();
    }
}
