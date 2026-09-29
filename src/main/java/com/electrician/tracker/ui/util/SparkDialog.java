package com.electrician.tracker.ui.util;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * The application's one message and question window, used instead of the
 * JavaFX {@code Alert}: borderless, a 4px strip and a single one-colour icon
 * in the colour of its {@link Kind}, bold title, grey description and at most
 * two buttons (the safe one outlined on the left, the action filled on the
 * right). The owner window is dimmed behind it. Esc closes it; Enter runs the
 * action except in delete confirmations, where only a click deletes. Focus
 * starts on the safe button. Colours come from the theme stylesheet.
 *
 * <pre>
 * SparkDialog.info("Kayıt güncellendi.");
 * SparkDialog.error("Şantiye silinemedi.", "Bu şantiyeye bağlı 12 malzeme kaydı var.");
 * boolean ok = SparkDialog.confirmDelete("12 kaydı silmek üzeresiniz", details, 12);
 * </pre>
 */
public final class SparkDialog {

    /** Above this many records a delete must be confirmed by typing the number. */
    public static final int TYPED_CONFIRMATION_THRESHOLD = 10;
    private static final Duration FADE_IN = Duration.millis(150);
    private static final double WIDE_WIDTH = 600;
    private static final double BODY_SPACING = 4;
    private static final double ICON_GAP = 14;
    private static final double BUTTON_GAP = 8;

    /** Kind of window: its colour, icon and whether Enter may run the action. */
    public enum Kind {
        INFO("spark-dialog-info", AppIcon.DIALOG_INFO),
        SUCCESS("spark-dialog-success", AppIcon.DIALOG_SUCCESS),
        WARNING("spark-dialog-warning", AppIcon.DIALOG_WARNING),
        ERROR("spark-dialog-error", AppIcon.DIALOG_ERROR),
        QUESTION("spark-dialog-info", AppIcon.DIALOG_QUESTION),
        DELETE("spark-dialog-error", AppIcon.DIALOG_WARNING);

        private final String styleClass;
        private final AppIcon icon;

        Kind(String styleClass, AppIcon icon) {
            this.styleClass = styleClass;
            this.icon = icon;
        }
    }

    private final Kind kind;
    private final String title;
    private final String detail;
    private final Node content;
    private final String primaryText;
    private final String secondaryText;
    private final int typedCount;
    private final boolean wide;
    private Stage stage;
    private boolean confirmed;

    private SparkDialog(Builder builder) {
        this.kind = builder.kind;
        this.title = builder.title;
        this.detail = builder.detail;
        this.content = builder.content;
        this.primaryText = builder.primaryText;
        this.secondaryText = builder.secondaryText;
        this.typedCount = builder.typedCount;
        this.wide = builder.wide;
    }

    // ---- Shortcuts ---------------------------------------------------------------------

    public static void info(String title) {
        info(title, null);
    }

    public static void info(String title, String detail) {
        message(Kind.INFO, title, detail);
    }

    public static void success(String title) {
        success(title, null);
    }

    public static void success(String title, String detail) {
        message(Kind.SUCCESS, title, detail);
    }

    public static void warning(String title, String detail) {
        message(Kind.WARNING, title, detail);
    }

    public static void error(String title) {
        error(title, null);
    }

    public static void error(String title, String detail) {
        message(Kind.ERROR, title, detail);
    }

    /** "Emin misiniz?": true when the user chose {@code confirmText}. */
    public static boolean confirm(String title, String detail, String confirmText, String cancelText) {
        return builder(Kind.QUESTION).title(title).detail(detail)
                .primary(confirmText).secondary(cancelText).build().showAndWait();
    }

    /** Yes/no question. */
    public static boolean confirm(String title, String detail) {
        return confirm(title, detail, DialogUtil.message("common.yes"), DialogUtil.message("common.no"));
    }

    /**
     * Irreversible delete of {@code count} records: the button says "12 Kaydı
     * Sil", and above {@value #TYPED_CONFIRMATION_THRESHOLD} records the number
     * must be typed first.
     */
    public static boolean confirmDelete(String title, String detail, int count) {
        return builder(Kind.DELETE).title(title).detail(detail)
                .primary(DialogUtil.message("dialog.delete.count", String.valueOf(count)))
                .secondary(DialogUtil.message("delete.cancel"))
                .typedCount(count > TYPED_CONFIRMATION_THRESHOLD ? count : 0)
                .build().showAndWait();
    }

    /** Irreversible delete of one thing, with its own button text ("Şantiyeyi Sil"). */
    public static boolean confirmDelete(String title, String detail, String deleteText) {
        return builder(Kind.DELETE).title(title).detail(detail).primary(deleteText)
                .secondary(DialogUtil.message("delete.cancel")).build().showAndWait();
    }

    /** A single line of text ("Şablon adı"); empty when cancelled or left blank. */
    public static Optional<String> askText(String title, String label) {
        TextField field = new TextField();
        Label caption = new Label(label);
        caption.getStyleClass().add("spark-dialog-detail");
        Platform.runLater(field::requestFocus);
        boolean ok = builder(Kind.QUESTION).title(title).content(new VBox(BODY_SPACING, caption, field))
                .primary(DialogUtil.message("action.ok")).secondary(DialogUtil.message("delete.cancel"))
                .build().showAndWait();
        return ok ? Optional.of(field.getText().trim()).filter(text -> !text.isEmpty()) : Optional.empty();
    }

    /** One of {@code items} from a drop-down; empty when cancelled. */
    public static <T> Optional<T> choose(String title, String label, List<T> items, Function<T, String> toText) {
        ComboBox<T> box = new ComboBox<>(FXCollections.observableArrayList(items));
        box.setMaxWidth(Double.MAX_VALUE);
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(T item) {
                return item == null ? "" : toText.apply(item);
            }

            @Override
            public T fromString(String text) {
                return null;
            }
        });
        box.getSelectionModel().selectFirst();
        Label caption = new Label(label);
        caption.getStyleClass().add("spark-dialog-detail");
        boolean ok = builder(Kind.QUESTION).title(title).content(new VBox(BODY_SPACING, caption, box))
                .primary(DialogUtil.message("action.ok")).secondary(DialogUtil.message("delete.cancel"))
                .build().showAndWait();
        return ok ? Optional.ofNullable(box.getValue()) : Optional.empty();
    }

    private static void message(Kind kind, String title, String detail) {
        builder(kind).title(title).detail(detail).primary(DialogUtil.message("action.ok")).build().showAndWait();
    }

    public static Builder builder(Kind kind) {
        return new Builder(kind);
    }

    // ---- Showing -------------------------------------------------------------------------

    /** Shows the window and waits; true when the action button was chosen. */
    public boolean showAndWait() {
        Window owner = activeWindow().orElse(null);
        VBox card = card();
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.setTitle(title);
        stage.initModality(Modality.APPLICATION_MODAL);
        StackPane root = new StackPane(card);
        root.setAlignment(Pos.CENTER);
        if (owner != null) {
            stage.initOwner(owner);
            root.getStyleClass().add("spark-dialog-scrim");
            root.setPrefSize(owner.getWidth(), owner.getHeight());
            stage.setX(owner.getX());
            stage.setY(owner.getY());
        }
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        Stylesheets.apply(scene);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);
        stage.setScene(scene);
        stage.setOnShown(event -> onShown(owner, root));
        stage.showAndWait();
        return confirmed;
    }

    /** Closes the window as if cancelled (e.g. a list inside it became empty). */
    public void close() {
        if (stage != null) {
            stage.close();
        }
    }

    private void onShown(Window owner, Node root) {
        if (owner == null) {
            stage.centerOnScreen();
        }
        FadeTransition fade = new FadeTransition(FADE_IN, root);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private VBox card() {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("spark-dialog-title");
        titleLabel.setWrapText(true);
        VBox text = new VBox(BODY_SPACING, titleLabel);
        if (detail != null && !detail.isBlank()) {
            Label detailLabel = new Label(detail);
            detailLabel.getStyleClass().add("spark-dialog-detail");
            detailLabel.setWrapText(true);
            text.getChildren().add(detailLabel);
        }
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox body = new HBox(ICON_GAP, Icons.of(kind.icon, IconSize.DIALOG), text);
        body.getStyleClass().add("spark-dialog-body");
        Region strip = new Region();
        strip.getStyleClass().add("spark-dialog-strip");
        VBox card = new VBox(strip, body);
        card.getStyleClass().addAll("spark-dialog", kind.styleClass);
        if (wide) {
            card.getStyleClass().add("spark-dialog-wide");
            card.setPrefWidth(WIDE_WIDTH);
        }
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        if (content != null) {
            VBox.setVgrow(content, Priority.ALWAYS);
            text.getChildren().add(content);
        }
        Button primary = primaryButton();
        if (typedCount > 0) {
            text.getChildren().add(typedCountBox(primary));
        }
        card.getChildren().add(buttonBar(primary));
        return card;
    }

    private Button primaryButton() {
        Button primary = new Button(primaryText);
        primary.getStyleClass().add("spark-dialog-primary");
        primary.setOnAction(event -> {
            confirmed = true;
            stage.close();
        });
        return primary;
    }

    private HBox buttonBar(Button primary) {
        HBox buttons = new HBox(BUTTON_GAP);
        buttons.getStyleClass().add("spark-dialog-buttons");
        Button focused = primary;
        if (secondaryText != null) {
            Button secondary = new Button(secondaryText);
            secondary.getStyleClass().add("spark-dialog-secondary");
            secondary.setOnAction(event -> stage.close());
            buttons.getChildren().add(secondary);
            focused = secondary;
        }
        buttons.getChildren().add(primary);
        Button initialFocus = focused;
        Platform.runLater(initialFocus::requestFocus);
        return buttons;
    }

    /** "Onaylamak için 12 yazın": the delete button stays disabled until the number is typed. */
    private VBox typedCountBox(Button primary) {
        Label hint = new Label(DialogUtil.message("dialog.delete.typeCount", String.valueOf(typedCount)));
        hint.getStyleClass().add("spark-dialog-detail");
        TextField field = new TextField();
        field.getStyleClass().add("spark-dialog-count-field");
        String expected = String.valueOf(typedCount);
        primary.disableProperty().bind(field.textProperty().isNotEqualTo(expected));
        return new VBox(BODY_SPACING, hint, field);
    }

    private void onKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            event.consume();
            stage.close();
        } else if (event.getCode() == KeyCode.ENTER) {
            event.consume();
            if (kind != Kind.DELETE) {
                confirmed = true;
                stage.close();
            }
        }
    }

    /** The focused window (a dialog or the main window), to dim and centre on. */
    private static Optional<Window> activeWindow() {
        return Window.getWindows().stream().filter(Window::isShowing).filter(Window::isFocused).findFirst()
                .or(() -> Window.getWindows().stream().filter(Window::isShowing).findFirst());
    }

    /** Builds a window with custom content (e.g. a list with row buttons). */
    public static final class Builder {

        private final Kind kind;
        private String title = "";
        private String detail;
        private Node content;
        private String primaryText;
        private String secondaryText;
        private int typedCount;
        private boolean wide;

        private Builder(Kind kind) {
            this.kind = kind;
            this.primaryText = DialogUtil.message("action.ok");
        }

        public Builder title(String value) {
            this.title = value;
            return this;
        }

        public Builder detail(String value) {
            this.detail = value;
            return this;
        }

        /** Extra content below the description (a list, a field). */
        public Builder content(Node value) {
            this.content = value;
            return this;
        }

        public Builder primary(String text) {
            this.primaryText = text;
            return this;
        }

        /** The safe button on the left; none when {@code null}. */
        public Builder secondary(String text) {
            this.secondaryText = text;
            return this;
        }

        public Builder typedCount(int count) {
            this.typedCount = count;
            return this;
        }

        /** 600px instead of 380–480px (lists). */
        public Builder wide() {
            this.wide = true;
            return this;
        }

        public SparkDialog build() {
            return new SparkDialog(this);
        }
    }
}
