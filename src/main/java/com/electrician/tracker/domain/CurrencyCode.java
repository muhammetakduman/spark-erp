package com.electrician.tracker.domain;

/**
 * Currency a material was bought in. Sales are always in lira; a purchase in
 * dollars or euros keeps its original amount and the exchange rate of the
 * purchase day next to its lira value.
 */
public enum CurrencyCode {
    TRY,
    USD,
    EUR;

    public boolean isForeign() {
        return this != TRY;
    }
}
