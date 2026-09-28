package com.electrician.tracker.ui.util;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;

/**
 * Gives FXML buttons their icon from a style class: {@code action-save}
 * → {@link AppIcon#SAVE}, {@code action-add} → {@link AppIcon#ADD} … Run once
 * after a view is loaded; content of scroll panes, tabs and titled panes
 * (which are not scene-graph children before they are shown) is included.
 */
public final class ButtonIcons {

    private static final String PREFIX = "action-";

    private ButtonIcons() {
    }

    public static void decorate(Node node) {
        if (node == null) {
            return;
        }
        if (node instanceof ButtonBase button && button.getGraphic() == null) {
            iconOf(button).ifPresent(icon -> Icons.decorate(button, icon));
        }
        if (node instanceof MenuButton menuButton) {
            menuButton.getItems().forEach(item -> item.getStyleClass().stream()
                    .filter(style -> style.startsWith(PREFIX))
                    .findFirst()
                    .flatMap(ButtonIcons::toIcon)
                    .ifPresent(icon -> Icons.decorate(item, icon)));
        }
        children(node).forEach(ButtonIcons::decorate);
    }

    private static Optional<AppIcon> iconOf(ButtonBase button) {
        return button.getStyleClass().stream()
                .filter(style -> style.startsWith(PREFIX))
                .findFirst()
                .flatMap(ButtonIcons::toIcon);
    }

    private static Optional<AppIcon> toIcon(String styleClass) {
        String name = styleClass.substring(PREFIX.length()).replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return Optional.of(AppIcon.valueOf(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static List<? extends Node> children(Node node) {
        if (node instanceof ScrollPane scrollPane) {
            return scrollPane.getContent() == null ? List.of() : List.of(scrollPane.getContent());
        }
        if (node instanceof TabPane tabPane) {
            return tabPane.getTabs().stream().map(Tab::getContent).filter(java.util.Objects::nonNull).toList();
        }
        if (node instanceof TitledPane titledPane) {
            return titledPane.getContent() == null ? List.of() : List.of(titledPane.getContent());
        }
        if (node instanceof Accordion accordion) {
            return accordion.getPanes();
        }
        if (node instanceof SplitPane splitPane) {
            return splitPane.getItems();
        }
        if (node instanceof ToolBar toolBar) {
            return toolBar.getItems();
        }
        if (node instanceof Parent parent) {
            return parent.getChildrenUnmodifiable();
        }
        return List.of();
    }
}
