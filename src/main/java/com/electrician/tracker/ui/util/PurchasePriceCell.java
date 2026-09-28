package com.electrician.tracker.ui.util;

import java.math.BigDecimal;
import java.util.function.Function;

import com.electrician.tracker.domain.CurrencyCode;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.VBox;

/**
 * A purchase price in lira; a dollar or euro purchase adds a small grey
 * second line with the original amount and rate:
 * <pre>
 * 4.110,00 ₺
 * 120,00 $ · kur 34,25
 * </pre>
 */
public final class PurchasePriceCell<T> extends TableCell<T, BigDecimal> {

    private final Function<T, ForeignAmount> foreign;

    public PurchasePriceCell(Function<T, ForeignAmount> foreign) {
        this.foreign = foreign;
        getStyleClass().add("money-cell");
    }

    @Override
    protected void updateItem(BigDecimal liraValue, boolean empty) {
        super.updateItem(liraValue, empty);
        setText(null);
        setGraphic(null);
        if (empty || getTableRow() == null || getTableRow().getItem() == null) {
            return;
        }
        ForeignAmount original = foreign.apply(getTableRow().getItem());
        if (original == null) {
            setText(Bicimlendirici.money(liraValue));
            return;
        }
        Label lira = new Label(Bicimlendirici.money(liraValue));
        Label detail = new Label(DialogUtil.message("material.foreignDetail",
                Bicimlendirici.money(original.amount(), original.currency()),
                Bicimlendirici.exchangeRate(original.rate())));
        detail.getStyleClass().add("foreign-detail");
        VBox lines = new VBox(lira, detail);
        lines.setAlignment(Pos.CENTER_RIGHT);
        setGraphic(lines);
    }

    /** The original foreign amount of a purchase; {@code null} for a lira purchase. */
    public record ForeignAmount(BigDecimal amount, CurrencyCode currency, BigDecimal rate) {
    }
}
