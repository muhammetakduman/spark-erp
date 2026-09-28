package com.electrician.tracker.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ResourceBundle;

import com.electrician.tracker.ui.util.ButtonIcons;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Loads FXML views with the Spring context as the controller factory, so
 * every controller is a managed Spring bean. Buttons get their icon from
 * their {@code action-…} style class.
 */
@Component
public class FxmlViewLoader {

    private static final String MESSAGES_BUNDLE = "messages_tr";

    private final ApplicationContext applicationContext;

    public FxmlViewLoader(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Parent load(String fxmlPath) {
        return loadWithController(fxmlPath).root();
    }

    /**
     * Loads a view and also returns its controller, so the caller can pass
     * data into it (e.g. the record being edited) before showing the stage.
     */
    public <T> LoadedView<T> loadWithController(String fxmlPath) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setControllerFactory(applicationContext::getBean);
        loader.setResources(ResourceBundle.getBundle(MESSAGES_BUNDLE));
        try {
            Parent root = loader.load();
            ButtonIcons.decorate(root);
            return new LoadedView<>(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load FXML view: " + fxmlPath, e);
        }
    }

    public record LoadedView<T>(Parent root, T controller) {
    }
}
