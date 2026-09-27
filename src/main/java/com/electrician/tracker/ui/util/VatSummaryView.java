package com.electrician.tracker.ui.util;

import java.util.ArrayList;
import java.util.List;

import com.electrician.tracker.dto.VatBreakdown;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

/**
 * Renders a {@link VatBreakdown} as the totals block used on site and
 * service screens:
 * <pre>
 * Ara toplam (KDV'siz):  10.000,00 ₺
 * KDV (%20):              2.000,00 ₺
 * GENEL TOPLAM:          12.000,00 ₺
 * </pre>
 * Without any VAT only the grand total row is shown.
 */
public final class VatSummaryView {

    private static final double COLUMN_GAP = 16;

    private VatSummaryView() {
    }

    public static GridPane build(VatBreakdown breakdown) {
        GridPane grid = new GridPane();
        grid.setHgap(COLUMN_GAP);
        grid.getStyleClass().add("vat-summary");
        fill(grid, breakdown);
        return grid;
    }

    public static void fill(GridPane grid, VatBreakdown breakdown) {
        grid.getChildren().clear();
        List<Node[]> rows = new ArrayList<>();
        if (breakdown.hasVat()) {
            rows.add(row(DialogUtil.message("vat.summary.subtotal"), Bicimlendirici.money(breakdown.excludingVat()), false));
            for (VatBreakdown.VatRateAmount rate : breakdown.rates()) {
                rows.add(row(VatLabels.rateLine(rate.rate()), Bicimlendirici.money(rate.vatAmount()), false));
            }
        }
        rows.add(row(DialogUtil.message("vat.summary.grandTotal"), Bicimlendirici.money(breakdown.includingVat()), true));
        for (int i = 0; i < rows.size(); i++) {
            grid.addRow(i, rows.get(i));
        }
    }

    private static Node[] row(String caption, String amount, boolean emphasized) {
        Label captionLabel = new Label(caption);
        Label amountLabel = new Label(amount);
        amountLabel.getStyleClass().add("vat-summary-amount");
        if (emphasized) {
            captionLabel.getStyleClass().add("vat-summary-total");
            amountLabel.getStyleClass().add("vat-summary-total");
        }
        return new Node[] { captionLabel, amountLabel };
    }
}
