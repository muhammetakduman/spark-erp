package com.electrician.tracker.ui.util;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import com.electrician.tracker.domain.DailyJobStatus;
import com.electrician.tracker.dto.DailyJobCard;
import com.electrician.tracker.dto.WeekPlan;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * The week at a glance: Monday–Sunday columns; the first row lists each
 * day's jobs as short cards, the rows below show where each employee is on
 * each day. Empty days are grey, busy ones carry their job count. Columns
 * share the width, so the grid follows the window size. Clicking a day opens
 * it in the day view.
 */
public final class WeekGridView {

    private static final double NAME_COLUMN_PERCENT = 13;
    private static final int DAYS = 7;
    private static final double CELL_SPACING = 4;

    private WeekGridView() {
    }

    public static GridPane build(WeekPlan plan, LocalDate today, Consumer<LocalDate> openDay) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("week-grid");
        grid.getColumnConstraints().add(percent(NAME_COLUMN_PERCENT));
        for (int i = 0; i < DAYS; i++) {
            grid.getColumnConstraints().add(percent((100 - NAME_COLUMN_PERCENT) / DAYS));
        }
        grid.add(cornerCell(), 0, 0);
        grid.add(rowTitle(DialogUtil.message("dailyPlan.week.jobs")), 0, 1);
        int column = 1;
        for (LocalDate day : plan.days()) {
            List<DailyJobCard> cards = plan.cardsOn(day).stream()
                    .filter(card -> card.status() != DailyJobStatus.CANCELLED).toList();
            grid.add(dayHeader(day, cards.size(), day.equals(today), openDay), column, 0);
            grid.add(jobsCell(cards, day, openDay), column, 1);
            column++;
        }
        int row = 2;
        for (WeekPlan.EmployeeWeek employee : plan.employees()) {
            grid.add(rowTitle(employee.name()), 0, row);
            column = 1;
            for (LocalDate day : plan.days()) {
                grid.add(employeeCell(employee.titlesOn(day)), column++, row);
            }
            row++;
        }
        return grid;
    }

    private static ColumnConstraints percent(double value) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setPercentWidth(value);
        constraints.setHgrow(Priority.ALWAYS);
        return constraints;
    }

    private static Node cornerCell() {
        Label corner = new Label();
        corner.getStyleClass().add("week-corner");
        corner.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return corner;
    }

    /** "Pzt 29.09" and "3 iş" (grey when empty); today is highlighted. */
    private static Node dayHeader(LocalDate day, int jobCount, boolean today, Consumer<LocalDate> openDay) {
        Label name = new Label(Bicimlendirici.shortDateWithDay(day));
        name.getStyleClass().add("week-day-name");
        Label count = new Label(jobCount == 0 ? DialogUtil.message("dailyPlan.week.free")
                : DialogUtil.message("dailyPlan.week.count", jobCount));
        count.getStyleClass().add("week-day-count");
        VBox header = new VBox(name, count);
        header.getStyleClass().addAll("week-day-header", jobCount == 0 ? "week-cell-empty" : "week-cell-filled");
        if (today) {
            header.getStyleClass().add("week-today");
        }
        header.setMaxWidth(Double.MAX_VALUE);
        header.setOnMouseClicked(event -> openDay.accept(day));
        return header;
    }

    private static Node jobsCell(List<DailyJobCard> cards, LocalDate day, Consumer<LocalDate> openDay) {
        VBox cell = new VBox(CELL_SPACING);
        cell.getStyleClass().addAll("week-cell", cards.isEmpty() ? "week-cell-empty" : "week-cell-filled");
        for (DailyJobCard card : cards) {
            Label shortCard = new Label(card.timeOfDay() == null ? card.title() : card.timeOfDay() + " " + card.title());
            shortCard.getStyleClass().addAll("week-job", "week-job-" + card.status().name().toLowerCase(
                    java.util.Locale.ROOT).replace('_', '-'));
            if (card.isUrgent()) {
                shortCard.getStyleClass().add("week-job-urgent");
            }
            shortCard.setWrapText(true);
            shortCard.setMaxWidth(Double.MAX_VALUE);
            cell.getChildren().add(shortCard);
        }
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        cell.setOnMouseClicked(event -> openDay.accept(day));
        return cell;
    }

    private static Node employeeCell(List<String> titles) {
        VBox cell = new VBox(CELL_SPACING);
        cell.getStyleClass().addAll("week-cell", titles.isEmpty() ? "week-cell-empty" : "week-cell-filled");
        for (String title : titles) {
            Label label = new Label(title);
            label.getStyleClass().add("week-person-job");
            label.setWrapText(true);
            cell.getChildren().add(label);
        }
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return cell;
    }

    private static Node rowTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("week-row-title");
        label.setWrapText(true);
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return label;
    }
}
