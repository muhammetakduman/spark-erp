package com.electrician.tracker.ui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.electrician.tracker.domain.DailyJobStatus;
import com.electrician.tracker.dto.DailyJobCard;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * One daily job as a card: time · title · status badge on top, customer,
 * address and phone below, the team as small badges, notes. An urgent job
 * has a red left edge and a red exclamation mark after its title. While the job is open the right side carries the two
 * big buttons "Gidildi" / "Gidilmedi" (and, for a job of a site or service,
 * "Puantaja da yaz", ticked); rarely used actions sit under "Daha fazla".
 */
public final class DailyJobCardView {

    private static final double SPACING = 6;
    private static final double CARD_SPACING = 14;
    private static final double BADGE_GAP = 4;

    private final Actions actions;
    private Function<DailyJobCard, Node> selectBox;

    public DailyJobCardView(Actions actions) {
        this.actions = actions;
    }

    /** A tick box at the left of every card, for bulk actions (ADMIN). */
    public void setSelectBox(Function<DailyJobCard, Node> value) {
        this.selectBox = value;
    }

    /** What the card's buttons do; implemented by the plan screen. */
    public interface Actions {

        void done(DailyJobCard card, boolean writeAttendance);

        void notDone(DailyJobCard card);

        void saveCompletionNote(DailyJobCard card, String note);

        void edit(DailyJobCard card);

        void cancel(DailyJobCard card);

        void delete(DailyJobCard card);

        void moveToToday(DailyJobCard card);
    }

    /** A card of the day view; {@code noteEditorOpen} shows the optional note field after "Gidildi". */
    public Node dayCard(DailyJobCard card, boolean noteEditorOpen) {
        VBox main = mainColumn(card, false);
        if (noteEditorOpen && card.status() == DailyJobStatus.COMPLETED) {
            main.getChildren().add(noteEditor(card));
        }
        return frame(card, main, card.isOpen() ? openActions(card) : closedActions(card));
    }

    /** A card of the "Bekleyen" list: its day is shown, the action is "Bugüne aktar". */
    public Node pendingCard(DailyJobCard card) {
        Button moveButton = Icons.button(AppIcon.TODAY, "dailyJob.action.moveToToday", () -> actions.moveToToday(card));
        moveButton.getStyleClass().add("primary-button");
        VBox side = new VBox(SPACING, moveButton, moreMenu(card));
        side.setAlignment(Pos.TOP_RIGHT);
        return frame(card, mainColumn(card, true), side);
    }

    private HBox frame(DailyJobCard card, VBox main, Node side) {
        HBox.setHgrow(main, Priority.ALWAYS);
        HBox box = new HBox(CARD_SPACING, main, side);
        if (selectBox != null) {
            box.getChildren().add(0, selectBox.apply(card));
        }
        box.getStyleClass().add("job-card");
        if (card.isUrgent()) {
            box.getStyleClass().add("job-card-urgent");
        }
        if (card.status() == DailyJobStatus.CANCELLED || card.status() == DailyJobStatus.POSTPONED) {
            box.getStyleClass().add("job-card-faded");
        }
        return box;
    }

    private VBox mainColumn(DailyJobCard card, boolean withDate) {
        VBox main = new VBox(SPACING, headerLine(card, withDate));
        placeLine(card).ifPresent(main.getChildren()::add);
        if (!card.employeeNames().isEmpty()) {
            main.getChildren().add(teamBadges(card));
        }
        if (card.jobLabel() != null) {
            main.getChildren().add(muted(DialogUtil.message("dailyJob.card.linkedJob", card.jobLabel())));
        }
        if (card.note() != null) {
            main.getChildren().add(muted(card.note()));
        }
        if (card.completionNote() != null) {
            main.getChildren().add(muted(DialogUtil.message("dailyJob.card.completionNote", card.completionNote())));
        }
        if (card.postponedTo() != null) {
            main.getChildren().add(muted(DialogUtil.message("dailyJob.card.movedTo",
                    Bicimlendirici.date(card.postponedTo()))));
        }
        return main;
    }

    /** "09:00 · Ahmet Bey – priz arızası (!)  [Planlandı] 2 kez ertelendi". */
    private HBox headerLine(DailyJobCard card, boolean withDate) {
        List<Node> parts = new ArrayList<>();
        if (withDate) {
            parts.add(styled(new Label(Bicimlendirici.dateWithDay(card.date())), "job-card-time"));
        }
        if (card.timeOfDay() != null) {
            parts.add(styled(new Label(card.timeOfDay(), Icons.of(AppIcon.CLOCK, IconSize.BUTTON)), "job-card-time"));
        }
        Label title = styled(new Label(card.title()), "job-card-title");
        title.setWrapText(true);
        if (card.isUrgent()) {
            title.setGraphic(StatusBadges.urgentMark());
            title.setContentDisplay(ContentDisplay.RIGHT);
        }
        parts.add(title);
        parts.add(StatusBadges.of(card.status()));
        if (card.postponeCount() > 0) {
            Label postponed = new Label(DialogUtil.message("dailyJob.card.postponed", card.postponeCount()));
            postponed.getStyleClass().add(card.isFrequentlyPostponed() ? "warning-badge" : "muted-text");
            parts.add(postponed);
        }
        HBox line = new HBox(SPACING * 2, parts.toArray(Node[]::new));
        line.setAlignment(Pos.CENTER_LEFT);
        return line;
    }

    private static java.util.Optional<Node> placeLine(DailyJobCard card) {
        List<Node> parts = new ArrayList<>();
        if (card.customerName() != null) {
            parts.add(new Label(card.customerName(), Icons.of(AppIcon.USER, IconSize.BUTTON)));
        }
        if (card.address() != null) {
            parts.add(new Label(card.address(), Icons.of(AppIcon.PLACE, IconSize.BUTTON)));
        }
        if (card.phone() != null) {
            parts.add(new Label(card.phone(), Icons.of(AppIcon.PHONE, IconSize.BUTTON)));
        }
        if (parts.isEmpty()) {
            return java.util.Optional.empty();
        }
        FlowPane line = new FlowPane(SPACING * 3, SPACING, parts.toArray(Node[]::new));
        line.getStyleClass().add("job-card-place");
        return java.util.Optional.of(line);
    }

    private static FlowPane teamBadges(DailyJobCard card) {
        FlowPane badges = new FlowPane(BADGE_GAP, BADGE_GAP);
        card.employeeNames().forEach(name -> badges.getChildren().add(styled(new Label(name), "person-badge")));
        return badges;
    }

    /** "Gidildi" / "Gidilmedi", the attendance option and "Daha fazla". */
    private VBox openActions(DailyJobCard card) {
        CheckBox attendance = new CheckBox(DialogUtil.message("dailyJob.writeAttendance"));
        attendance.setSelected(true);
        boolean canWrite = card.willHaveJob() && !card.employeeIds().isEmpty();
        Button done = Icons.button(AppIcon.DONE, "dailyJob.action.done",
                () -> actions.done(card, canWrite && attendance.isSelected()));
        done.getStyleClass().add("done-button");
        Button notDone = Icons.button(AppIcon.NOT_DONE, "dailyJob.action.notDone", () -> actions.notDone(card));
        notDone.getStyleClass().add("not-done-button");
        for (Button button : List.of(done, notDone)) {
            button.setMaxWidth(Double.MAX_VALUE);
        }
        VBox side = new VBox(SPACING, done, notDone);
        if (canWrite) {
            side.getChildren().add(attendance);
        }
        side.getChildren().add(moreMenu(card));
        side.setAlignment(Pos.TOP_RIGHT);
        side.getStyleClass().add("job-card-actions");
        return side;
    }

    private VBox closedActions(DailyJobCard card) {
        VBox side = new VBox(SPACING, moreMenu(card));
        side.setAlignment(Pos.TOP_RIGHT);
        return side;
    }

    private MenuButton moreMenu(DailyJobCard card) {
        MenuButton more = new MenuButton(DialogUtil.message("action.more"));
        more.getStyleClass().add("more-button");
        if (card.isOpen()) {
            more.getItems().addAll(
                    menuItem("action.edit", AppIcon.EDIT, () -> actions.edit(card)),
                    menuItem("dailyJob.action.cancel", AppIcon.CANCEL, () -> actions.cancel(card)));
        }
        more.getItems().add(menuItem("action.delete", AppIcon.DELETE, () -> actions.delete(card)));
        return more;
    }

    /** The optional note right after "Gidildi"; the job is already done without it. */
    private HBox noteEditor(DailyJobCard card) {
        TextField note = new TextField(card.completionNote());
        note.setPromptText(DialogUtil.message("dailyJob.completionNote.prompt"));
        HBox.setHgrow(note, Priority.ALWAYS);
        Button save = Icons.button(AppIcon.SAVE, "action.save", () -> actions.saveCompletionNote(card,
                note.getText()));
        note.setOnAction(event -> actions.saveCompletionNote(card, note.getText()));
        HBox editor = new HBox(SPACING, note, save);
        editor.setAlignment(Pos.CENTER_LEFT);
        editor.getStyleClass().add("completion-note-editor");
        return editor;
    }

    private static MenuItem menuItem(String key, AppIcon icon, Runnable action) {
        MenuItem item = new MenuItem(DialogUtil.message(key));
        item.setOnAction(event -> action.run());
        return Icons.decorate(item, icon);
    }

    private static Label muted(String text) {
        Label label = styled(new Label(text), "muted-text");
        label.setWrapText(true);
        return label;
    }

    private static <T extends javafx.scene.control.Labeled> T styled(T node, String styleClass) {
        node.getStyleClass().add(styleClass);
        return node;
    }
}
