package com.electrician.tracker.ui.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Selecting many rows of a list for a bulk action: a tick box per row, the
 * header box ticking every <em>visible</em> (filtered) row, Shift+click for a
 * range, Ctrl+A for all visible rows and Esc to clear. While something is
 * ticked a strip above the list says "12 kayıt seçildi" with "Seçimi Temizle"
 * and "Seçilenleri Sil". The selection is cleared whenever the list's rows
 * change (filter, page, reload), so nothing hidden can be deleted by mistake.
 */
public final class BulkSelection<T> {

    private static final KeyCombination SELECT_ALL = new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN);
    private static final double BAR_SPACING = 6;
    private static final double COLUMN_WIDTH = 36;
    private static final String FOLLOW_KEY = "bulk-selection-follow";

    private final Supplier<List<T>> visibleItems;
    private final Function<T, Long> idOf;
    private final Map<Long, BooleanProperty> marks = new HashMap<>();
    private final ReadOnlyIntegerWrapper count = new ReadOnlyIntegerWrapper(0);
    private final HBox bar;
    private int anchorIndex = -1;

    /**
     * @param visibleItems the rows as shown now (after filters), in display order
     * @param onDelete     receives the ticked visible rows for "Seçilenleri Sil"
     */
    public BulkSelection(Supplier<List<T>> visibleItems, Function<T, Long> idOf, Consumer<List<T>> onDelete) {
        this.visibleItems = visibleItems;
        this.idOf = idOf;
        this.bar = buildBar(onDelete);
    }

    /** A table gets the tick column, the strip above it and the shortcuts. */
    public static <T> BulkSelection<T> forTable(TableView<T> table, Function<T, Long> idOf,
            Consumer<List<T>> onDelete) {
        BulkSelection<T> selection = new BulkSelection<>(table::getItems, idOf, onDelete);
        table.getColumns().add(0, selection.column());
        selection.placeAbove(table);
        selection.installKeys(table);
        table.itemsProperty().addListener((obs, old, items) -> {
            selection.clear();
            items.addListener(selection.clearOnChange());
        });
        table.getItems().addListener(selection.clearOnChange());
        return selection;
    }

    public ReadOnlyIntegerProperty countProperty() {
        return count.getReadOnlyProperty();
    }

    /** The ticked rows among those visible now, in display order. */
    public List<T> selected() {
        return visibleItems.get().stream().filter(item -> markOf(item).get()).toList();
    }

    public void selectAllVisible() {
        visibleItems.get().forEach(item -> markOf(item).set(true));
    }

    public void clear() {
        marks.values().forEach(mark -> mark.set(false));
        anchorIndex = -1;
    }

    /**
     * A tick box for one row of a non-table list (cards, accordion panes);
     * its clicks do not reach the row (e.g. never open an accordion pane).
     */
    public CheckBox checkBoxFor(T item) {
        CheckBox box = new CheckBox();
        box.getStyleClass().add("select-box");
        bind(box, item);
        box.addEventHandler(MouseEvent.ANY, MouseEvent::consume);
        return box;
    }

    /** The strip with the count and the two buttons; hidden while nothing is ticked. */
    public HBox bar() {
        return bar;
    }

    /** Ctrl+A ticks every visible row, Esc clears. */
    public void installKeys(Node node) {
        node.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl) {
                return;
            }
            if (SELECT_ALL.match(event)) {
                selectAllVisible();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE && count.get() > 0) {
                clear();
                event.consume();
            }
        });
    }

    /** Clears the selection on real changes of the rows (not on sorting). */
    public ListChangeListener<T> clearOnChange() {
        return change -> {
            while (change.next()) {
                if (!change.wasPermutated()) {
                    clear();
                    return;
                }
            }
        };
    }

    /**
     * Puts the strip right above {@code list}, taking the place of the list in
     * its parent. Call it from {@code initialize()}, before the parent's skin exists.
     */
    public void placeAbove(Node list) {
        Parent parent = list.getParent();
        VBox wrapper = new VBox(BAR_SPACING);
        VBox.setVgrow(wrapper, VBox.getVgrow(list));
        HBox.setHgrow(wrapper, HBox.getHgrow(list));
        if (parent instanceof BorderPane border && border.getCenter() == list) {
            border.setCenter(wrapper);
        } else if (parent instanceof SplitPane split) {
            split.getItems().set(split.getItems().indexOf(list), wrapper);
        } else if (parent instanceof Pane pane) {
            pane.getChildren().set(pane.getChildren().indexOf(list), wrapper);
        }
        VBox.setVgrow(list, Priority.ALWAYS);
        wrapper.getChildren().setAll(bar, list);
    }

    private TableColumn<T, Boolean> column() {
        TableColumn<T, Boolean> column = new TableColumn<>();
        column.getStyleClass().add("select-column");
        column.setSortable(false);
        column.setReorderable(false);
        column.setResizable(false);
        column.setMinWidth(COLUMN_WIDTH);
        column.setMaxWidth(COLUMN_WIDTH);
        column.setPrefWidth(COLUMN_WIDTH);
        column.setGraphic(headerBox());
        column.setCellFactory(col -> new TickCell());
        return column;
    }

    /** Ticked when every visible row is ticked; a click ticks all visible rows or clears. */
    private CheckBox headerBox() {
        CheckBox header = new CheckBox();
        header.setTooltip(new javafx.scene.control.Tooltip(DialogUtil.message("bulk.selectAllVisible")));
        count.addListener((obs, old, value) -> header.setSelected(allVisibleTicked()));
        header.setOnAction(event -> {
            if (header.isSelected()) {
                selectAllVisible();
            } else {
                clear();
            }
        });
        return header;
    }

    private boolean allVisibleTicked() {
        List<T> visible = visibleItems.get();
        return !visible.isEmpty() && visible.stream().allMatch(item -> markOf(item).get());
    }

    private BooleanProperty markOf(T item) {
        return marks.computeIfAbsent(idOf.apply(item), id -> {
            BooleanProperty mark = new SimpleBooleanProperty(false);
            mark.addListener((obs, old, ticked) -> count.set(count.get() + (ticked ? 1 : -1)));
            return mark;
        });
    }

    /** Box ↔ mark; Shift+click ticks (or unticks) the whole range from the last clicked row. */
    private void bind(CheckBox box, T item) {
        boolean[] shift = { false };
        box.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> shift[0] = event.isShiftDown());
        box.setSelected(markOf(item).get());
        ChangeListener<Boolean> follow = (obs, old, ticked) -> box.setSelected(ticked);
        box.getProperties().put(FOLLOW_KEY, follow);
        markOf(item).addListener(new WeakChangeListener<>(follow));
        box.setOnAction(event -> {
            int index = visibleItems.get().indexOf(item);
            if (shift[0] && anchorIndex >= 0 && index >= 0) {
                tickRange(anchorIndex, index, box.isSelected());
            } else {
                markOf(item).set(box.isSelected());
            }
            anchorIndex = index;
            shift[0] = false;
        });
    }

    private void tickRange(int from, int to, boolean ticked) {
        List<T> visible = visibleItems.get();
        for (int i = Math.min(from, to); i <= Math.max(from, to) && i < visible.size(); i++) {
            markOf(visible.get(i)).set(ticked);
        }
    }

    private HBox buildBar(Consumer<List<T>> onDelete) {
        Label countLabel = new Label();
        countLabel.getStyleClass().add("selection-count");
        countLabel.textProperty().bind(count.asString(DialogUtil.message("bulk.selectedCount")));
        Button clearButton = Icons.button(AppIcon.SELECT_CLEAR, "bulk.clearSelection", this::clear);
        Button deleteButton = Icons.button(AppIcon.DELETE, "bulk.deleteSelected", () -> onDelete.accept(selected()));
        deleteButton.getStyleClass().add("danger-button");
        HBox strip = new HBox(countLabel, clearButton, deleteButton);
        strip.getStyleClass().add("selection-bar");
        strip.setAlignment(Pos.CENTER_LEFT);
        strip.visibleProperty().bind(count.greaterThan(0));
        strip.managedProperty().bind(strip.visibleProperty());
        return strip;
    }

    /** The tick cell of a table row; follows cell reuse. */
    private final class TickCell extends TableCell<T, Boolean> {

        @Override
        protected void updateItem(Boolean value, boolean empty) {
            super.updateItem(value, empty);
            T item = empty || getTableRow() == null ? null : getTableRow().getItem();
            setGraphic(item == null ? null : checkBoxFor(item));
            setText(null);
        }
    }
}
