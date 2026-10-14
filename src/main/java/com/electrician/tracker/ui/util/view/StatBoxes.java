package com.electrician.tracker.ui.util.view;

import com.electrician.tracker.dto.job.JobSummary;
import com.electrician.tracker.ui.util.dialog.DialogUtil;
import com.electrician.tracker.ui.util.format.Bicimlendirici;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Small "title / big value / optional caption" boxes used for the main-screen
 * cards and a job's figures, so every total reads the same way. A job's row
 * shows the VAT split under its grand total instead of a separate table:
 * <pre>
 * Toplam (KDV dahil)   Tahsil Edilen   Kalan        Kâr (KDV hariç)
 * 12.000,00 ₺          5.000,00 ₺      7.000,00 ₺   3.000,00 ₺
 * KDV'siz 10.000 + KDV 2.000
 * </pre>
 * A site taken on for an agreed figure gains one more box between the grand
 * total and what has been collected: how much of that figure the charges so
 * far leave untouched.
 */
public final class StatBoxes {

    private static final double BOX_SPACING = 2;
    private static final double ROW_SPACING = 12;

    private StatBoxes() {
    }

    /** One box; {@code caption} may be {@code null}. */
    public static VBox box(String title, String value, String caption) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        VBox box = new VBox(BOX_SPACING, titleLabel, valueLabel);
        box.getStyleClass().add("stat-box");
        if (caption != null) {
            Label captionLabel = new Label(caption);
            captionLabel.getStyleClass().add("stat-caption");
            box.getChildren().add(captionLabel);
        }
        return box;
    }

    /**
     * Grand total (with its VAT split), collected, remaining and profit of one
     * job; only the grand total when the summary carries no financial figures.
     */
    public static HBox jobRow(JobSummary summary) {
        VBox total = box(DialogUtil.message("home.stat.total"), Bicimlendirici.money(summary.saleIncludingVat()),
                vatCaption(summary));
        if (!summary.hasFinancials()) {
            HBox row = new HBox(ROW_SPACING, total);
            row.getStyleClass().add("stat-row");
            return row;
        }
        VBox remaining = box(DialogUtil.message("home.stat.remaining"), Bicimlendirici.money(summary.remaining()), null);
        remaining.getStyleClass().add(summary.isFullyPaid() ? "stat-box-ok" : "stat-box-due");
        HBox row = new HBox(ROW_SPACING, total);
        if (summary.hasInitialBalance()) {
            row.getChildren().add(initialBalanceBox(summary));
        }
        row.getChildren().addAll(
                box(DialogUtil.message("home.stat.collected"), Bicimlendirici.money(summary.collectedTotal()), null),
                remaining,
                profitBox(summary));
        row.getStyleClass().add("stat-row");
        return row;
    }

    /** What is left of the agreed figure, with that figure itself underneath. */
    private static VBox initialBalanceBox(JobSummary summary) {
        VBox box = box(DialogUtil.message("home.stat.initialBalance"),
                Bicimlendirici.money(summary.initialBalanceRemaining()),
                DialogUtil.message("home.stat.initialBalanceStart",
                        Bicimlendirici.money(summary.initialBalance())));
        // Only an overrun is marked; a healthy balance needs no colour of its own.
        if (summary.isInitialBalanceOverrun()) {
            box.getStyleClass().add("stat-box-due");
        }
        return box;
    }

    private static String vatCaption(JobSummary summary) {
        if (!summary.saleVat().hasVat()) {
            return DialogUtil.message("home.stat.noVat");
        }
        return DialogUtil.message("home.stat.vatSplit", Bicimlendirici.money(summary.saleExcludingVat()),
                Bicimlendirici.money(summary.saleVatAmount()));
    }

    private static VBox profitBox(JobSummary summary) {
        String caption = summary.isProfitEstimated() ? DialogUtil.message("home.stat.profitEstimated") : null;
        VBox box = box(DialogUtil.message("home.stat.profit"), Bicimlendirici.money(summary.profit()), caption);
        if (summary.isProfitEstimated()) {
            Tooltip.install(box, new Tooltip(DialogUtil.message("home.stat.missingPurchasePrice",
                    String.valueOf(summary.materialPurchasePriceMissingCount()))));
        }
        return box;
    }
}
