package com.electrician.tracker.ui.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.electrician.tracker.dto.VatBreakdown;
import com.electrician.tracker.service.KdvHesaplayici;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

/**
 * Per-price VAT picker: one drop-down ("KDV yok", "%20 KDV hariç",
 * "%20 KDV dahil", …). When bound to an amount field it also shows the other
 * side of the price, e.g. "KDV'li: 1.200,00 ₺", so the user sees at once what
 * the choice means. Usable directly from FXML.
 */
public class VatSelector extends HBox {

    private static final List<Integer> RATES = List.of(20, 10, 1);
    private static final double SPACING = 8;

    private final ComboBox<Option> combo = new ComboBox<>();
    private final Label hintLabel = new Label();
    private DecimalField amountField;
    private Runnable onChange = () -> { };

    public VatSelector() {
        super(SPACING);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("vat-selector");
        hintLabel.getStyleClass().add("vat-hint");
        combo.getItems().setAll(options());
        combo.setConverter(new OptionConverter());
        combo.setValue(Option.NONE);
        combo.valueProperty().addListener((obs, o, n) -> changed());
        getChildren().addAll(combo, hintLabel);
        refreshHint();
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange == null ? () -> { } : onChange;
    }

    /** Shows the VAT-inclusive (or -exclusive) counterpart of this field's amount next to the picker. */
    public void bindAmount(DecimalField field) {
        this.amountField = field;
        field.textProperty().addListener((obs, o, n) -> refreshHint());
        refreshHint();
    }

    /** The chosen rate, or {@code null} for no VAT. */
    public Integer getRate() {
        return selected().rate();
    }

    public Boolean getIncluded() {
        return selected().rate() == null ? null : selected().included();
    }

    public void setValue(Integer rate, Boolean included) {
        combo.setValue(rate == null ? Option.NONE : new Option(rate, Boolean.TRUE.equals(included)));
    }

    public void clear() {
        combo.setValue(Option.NONE);
    }

    private Option selected() {
        return Objects.requireNonNullElse(combo.getValue(), Option.NONE);
    }

    private void changed() {
        refreshHint();
        onChange.run();
    }

    private void refreshHint() {
        String hint = hintText();
        hintLabel.setText(hint);
        hintLabel.setVisible(!hint.isEmpty());
        hintLabel.setManaged(!hint.isEmpty());
    }

    private String hintText() {
        BigDecimal amount = amountValue();
        Option option = selected();
        if (amount == null || option.rate() == null) {
            return "";
        }
        VatBreakdown breakdown = KdvHesaplayici.calculate(
                List.of(new KdvHesaplayici.Line(amount, option.rate(), option.included())));
        return option.included()
                ? DialogUtil.message("vat.hint.withoutVat", Bicimlendirici.money(breakdown.excludingVat()))
                : DialogUtil.message("vat.hint.withVat", Bicimlendirici.money(breakdown.includingVat()));
    }

    private BigDecimal amountValue() {
        if (amountField == null) {
            return null;
        }
        try {
            return amountField.getValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<Option> options() {
        List<Option> options = new ArrayList<>();
        options.add(Option.NONE);
        for (Integer rate : RATES) {
            options.add(new Option(rate, false));
            options.add(new Option(rate, true));
        }
        return options;
    }

    /** A rate with included/excluded; {@link #NONE} means no VAT. */
    private record Option(Integer rate, boolean included) {
        static final Option NONE = new Option(null, false);
    }

    private static final class OptionConverter extends StringConverter<Option> {
        @Override
        public String toString(Option option) {
            if (option == null || option.rate() == null) {
                return DialogUtil.message("vat.option.none");
            }
            return DialogUtil.message(option.included() ? "vat.option.including" : "vat.option.excluding",
                    String.valueOf(option.rate()));
        }

        @Override
        public Option fromString(String string) {
            return null;
        }
    }
}
