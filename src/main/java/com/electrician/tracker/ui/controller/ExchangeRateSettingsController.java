package com.electrician.tracker.ui.controller;

import java.math.BigDecimal;
import java.util.Objects;

import com.electrician.tracker.domain.CurrencyCode;
import com.electrician.tracker.dto.ExchangeRate;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.ExchangeRateService;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.DecimalField;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.UnsavedChangesAware;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Güncel kurlar" in Settings (ADMIN only): the dollar and euro rates typed
 * by hand, with the day they were last changed. New foreign-currency
 * purchases suggest them; saved purchases keep their own rate.
 */
@Component
@Scope("prototype")
public class ExchangeRateSettingsController implements UnsavedChangesAware {

    private final ExchangeRateService exchangeRateService;
    private final AccessControl accessControl;

    @FXML
    private VBox ratesSection;
    @FXML
    private DecimalField usdField;
    @FXML
    private DecimalField eurField;
    @FXML
    private Label usdUpdatedLabel;
    @FXML
    private Label eurUpdatedLabel;

    private BigDecimal shownUsd;
    private BigDecimal shownEur;

    public ExchangeRateSettingsController(ExchangeRateService exchangeRateService, AccessControl accessControl) {
        this.exchangeRateService = exchangeRateService;
        this.accessControl = accessControl;
    }

    @FXML
    private void initialize() {
        boolean admin = accessControl.isAdmin();
        ratesSection.setVisible(admin);
        ratesSection.setManaged(admin);
        if (admin) {
            refresh();
        }
    }

    @Override
    public boolean hasUnsavedChanges() {
        try {
            return !same(shownUsd, usdField.getValue()) || !same(shownEur, eurField.getValue());
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private void refresh() {
        shownUsd = show(CurrencyCode.USD, usdField, usdUpdatedLabel);
        shownEur = show(CurrencyCode.EUR, eurField, eurUpdatedLabel);
    }

    private BigDecimal show(CurrencyCode currency, DecimalField field, Label updatedLabel) {
        ExchangeRate rate = exchangeRateService.find(currency).orElse(null);
        field.setValue(rate == null ? null : rate.rate());
        updatedLabel.setText(rate == null || rate.updatedOn() == null ? DialogUtil.message("settings.rates.never")
                : DialogUtil.message("settings.rates.updatedOn", Bicimlendirici.date(rate.updatedOn())));
        return rate == null ? null : rate.rate();
    }

    @FXML
    private void onSave() {
        try {
            save(CurrencyCode.USD, usdField.getValue(), shownUsd);
            save(CurrencyCode.EUR, eurField.getValue(), shownEur);
            refresh();
            DialogUtil.showInfo("settings.rates.saved");
        } catch (NumberFormatException e) {
            DialogUtil.showErrorMessage("error.exchangeRate.required");
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    /** Only a changed, filled-in rate is stored (its date becomes today). */
    private void save(CurrencyCode currency, BigDecimal value, BigDecimal shown) {
        if (value != null && !same(value, shown)) {
            exchangeRateService.update(currency, value);
        }
    }

    private static boolean same(BigDecimal first, BigDecimal second) {
        if (first == null || second == null) {
            return Objects.equals(first, second);
        }
        return first.compareTo(second) == 0;
    }
}
