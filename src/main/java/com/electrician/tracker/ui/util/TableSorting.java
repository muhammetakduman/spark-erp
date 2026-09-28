package com.electrician.tracker.ui.util;

import java.math.BigDecimal;
import java.text.Collator;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;

import com.electrician.tracker.domain.Attendance;
import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.dto.EmployeeWageSummary;
import com.electrician.tracker.dto.MonthlyReportRow;
import com.electrician.tracker.dto.ProductOverview;
import com.electrician.tracker.dto.ProductUsageRow;
import com.electrician.tracker.dto.QuoteRow;
import com.electrician.tracker.dto.SupplierPriceComparison;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * The one place for table sorting ("SiralamaYardimcisi"):
 * <ul>
 *   <li>every table's default order (items are handed to the table already in
 *       it, so clicking a header a third time returns to it);</li>
 *   <li>typed column binding, so a click sorts numbers as numbers, dates as
 *       dates and text in Turkish alphabetical order (C, Ç, … S, Ş);</li>
 *   <li>helpers for filtered lists and pinned total rows.</li>
 * </ul>
 * Quote lines are the only exception: they keep the user's order.
 */
public final class TableSorting {

    private static final String MONEY_CELL_STYLE = "money-cell";

    private TableSorting() {
    }

    // ---- Text -------------------------------------------------------------------

    /** Turkish alphabetical order, {@code null}/blank last. */
    public static Comparator<String> turkishText() {
        Collator collator = Collator.getInstance(Bicimlendirici.TURKISH);
        return Comparator.nullsLast((left, right) -> collator.compare(left, right));
    }

    private static <T> Comparator<T> byText(Function<T, String> text) {
        return Comparator.comparing(item -> blankToNull(text.apply(item)), turkishText());
    }

    // ---- Default orders ---------------------------------------------------------

    /** Active sites first, then the newest start date. */
    public static Comparator<Job> sites() {
        return Comparator.comparing((Job job) -> job.getStatus() != JobStatus.ACTIVE)
                .thenComparing(Job::getStartDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(byText(job -> job.getCustomer().getName()));
    }

    /** Newest service first. */
    public static Comparator<Job> services() {
        return Comparator.comparing(Job::getStartDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Job::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    /** A job's material lines: category, then product name (A–Z). */
    public static Comparator<MaterialItem> materialLines() {
        Comparator<Product> categoryThenName = byText(Product::getCategory)
                .thenComparing(byText(Product::getName))
                .thenComparing(byText(Product::getBrand));
        return Comparator.comparing(MaterialItem::getProduct, categoryThenName);
    }

    /** Raw attendance: oldest date first, then employee name. */
    public static Comparator<Attendance> attendanceRows() {
        return Comparator.comparing(Attendance::getAttendanceDate)
                .thenComparing(byText(attendance -> attendance.getEmployee().getName()));
    }

    /** Per-employee attendance: most days first. */
    public static Comparator<EmployeeWageSummary> attendanceSummary() {
        return Comparator.comparing(EmployeeWageSummary::dayCount, Comparator.reverseOrder())
                .thenComparing(byText(EmployeeWageSummary::employeeName));
    }

    /** Newest payment first. */
    public static Comparator<Payment> payments() {
        return Comparator.comparing(Payment::getPaymentDate, Comparator.reverseOrder());
    }

    /** Category, brand, then name (A–Z). */
    public static Comparator<Product> products() {
        return byText(Product::getCategory)
                .thenComparing(byText(Product::getBrand))
                .thenComparing(byText(Product::getName));
    }

    public static Comparator<ProductOverview> productOverviews() {
        return Comparator.comparing(ProductOverview::product, products());
    }

    /** Inside one category: cheapest lowest purchase price first, unknown prices last. */
    public static Comparator<ProductOverview> cheapestFirst() {
        return Comparator.comparing(ProductOverview::lowestPurchasePrice, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(productOverviews());
    }

    public static Comparator<Customer> customers() {
        return byText(Customer::getName);
    }

    /** Masters first, then name. */
    public static Comparator<Employee> employees() {
        return Comparator.comparing((Employee employee) -> !employee.isMaster()).thenComparing(byText(Employee::getName));
    }

    /** Newest use first. */
    public static Comparator<ProductUsageRow> usageRows() {
        return Comparator.comparing(ProductUsageRow::date, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    /** Cheapest average purchase price first. */
    public static Comparator<SupplierPriceComparison> supplierComparison() {
        return Comparator.comparing(SupplierPriceComparison::averagePrice)
                .thenComparing(byText(SupplierPriceComparison::supplierName));
    }

    /** Oldest job of the month first. */
    public static Comparator<MonthlyReportRow> monthlyReportRows() {
        return Comparator.comparing(MonthlyReportRow::date, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /** Newest quote first. */
    public static Comparator<QuoteRow> quotes() {
        return Comparator.comparing(QuoteRow::quoteDate, Comparator.reverseOrder())
                .thenComparing(QuoteRow::quoteNo, Comparator.reverseOrder());
    }

    public static <T> ObservableList<T> sorted(java.util.Collection<T> items, Comparator<? super T> order) {
        return FXCollections.observableArrayList(items.stream().sorted(order).toList());
    }

    // ---- Typed columns ----------------------------------------------------------

    /** A text column sorted in Turkish alphabetical order. */
    public static <T> void text(TableColumn<T, String> column, Function<T, String> value) {
        column.setCellValueFactory(data -> new SimpleStringProperty(value.apply(data.getValue())));
        column.setComparator(turkishText());
    }

    /** An amount shown as "12.450,50 ₺" and sorted as a number. */
    public static <T> void money(TableColumn<T, BigDecimal> column, Function<T, BigDecimal> value) {
        number(column, value, Bicimlendirici::money);
        column.getStyleClass().add(MONEY_CELL_STYLE);
    }

    /** A number shown with {@code format} and sorted as a number; {@code null} values sort last. */
    public static <T> void number(TableColumn<T, BigDecimal> column, Function<T, BigDecimal> value,
            Function<BigDecimal, String> format) {
        typed(column, value, format);
    }

    public static <T> void date(TableColumn<T, LocalDate> column, Function<T, LocalDate> value) {
        typed(column, value, Bicimlendirici::date);
    }

    public static <T> void integer(TableColumn<T, Integer> column, Function<T, Integer> value) {
        typed(column, value, String::valueOf);
    }

    /**
     * A column whose text needs the whole row (e.g. "Kalan 700,00 ₺" or a
     * "*" mark) but that still sorts by {@code order}.
     */
    public static <T> void rowText(TableColumn<T, T> column, Function<T, String> text, Comparator<T> order) {
        column.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        column.setCellFactory(col -> new FormattedCell<>(text));
        column.setComparator(Comparator.nullsLast(order));
    }

    private static <T, V extends Comparable<? super V>> void typed(TableColumn<T, V> column, Function<T, V> value,
            Function<V, String> format) {
        column.setCellValueFactory(data -> new SimpleObjectProperty<>(value.apply(data.getValue())));
        column.setCellFactory(col -> new FormattedCell<>(format));
        column.setComparator(Comparator.nullsLast(Comparator.naturalOrder()));
    }

    // ---- Lists -----------------------------------------------------------------

    /**
     * Shows {@code source} (e.g. a filtered list) sorted by the clicked
     * columns; with no sort column the source order (the default) is kept.
     */
    public static <T> void bindSorted(TableView<T> table, ObservableList<T> source) {
        SortedList<T> sorted = new SortedList<>(source);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
    }

    /** Rows matching {@code pinned} (e.g. a TOTAL row) always stay at the bottom, whatever column is sorted. */
    public static <T> void pinLast(TableView<T> table, Predicate<T> pinned) {
        table.setSortPolicy(view -> {
            Comparator<T> byColumns = view.getComparator();
            Comparator<T> order = Comparator.comparing(pinned::test);
            if (byColumns != null) {
                order = order.thenComparing(byColumns);
            }
            FXCollections.sort(view.getItems(), order);
            return true;
        });
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }

    private static final class FormattedCell<T, V> extends TableCell<T, V> {

        private final Function<V, String> format;

        private FormattedCell(Function<V, String> format) {
            this.format = format;
        }

        @Override
        protected void updateItem(V item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : format.apply(item));
            setGraphic(null);
        }
    }
}
