package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.dto.ProductOverview;
import com.electrician.tracker.dto.ProductUsageRow;
import com.electrician.tracker.dto.ProductUsageSummary;
import com.electrician.tracker.dto.SupplierPrice;
import com.electrician.tracker.dto.SupplierPriceComparison;

/**
 * Pure purchase statistics of products ("hangi kabloyu kimden daha ucuza
 * almışım"). Purchase prices are always compared VAT-exclusive, whatever VAT
 * setting they were entered with; money is rounded HALF_UP to 2 digits.
 */
public final class PurchaseStatistics {

    private static final int MONEY_SCALE = 2;
    private static final Comparator<MaterialItem> NEWEST_LINE_FIRST = Comparator
            .comparing(MaterialItem::getItemDate, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(MaterialItem::getId, Comparator.nullsLast(Comparator.reverseOrder()));

    private PurchaseStatistics() {
    }

    /** The VAT-exclusive unit purchase price in lira; {@code null} when the line has none. */
    public static BigDecimal unitPriceExcludingVat(MaterialItem item) {
        return unitPriceExcludingVat(item.getPurchaseUnitPriceTl(), item.getPurchaseVatRate(),
                item.getPurchaseVatIncluded());
    }

    public static BigDecimal unitPriceExcludingVat(BigDecimal price, Integer vatRate, Boolean vatIncluded) {
        if (price == null) {
            return null;
        }
        return KdvHesaplayici.calculate(List.of(new KdvHesaplayici.Line(price, vatRate, vatIncluded))).excludingVat();
    }

    /** Statistics of one product from all its material lines (in any order). */
    public static ProductOverview overview(Product product, List<MaterialItem> lines) {
        List<MaterialItem> priced = lines.stream().filter(item -> item.getPurchaseUnitPriceTl() != null).toList();
        BigDecimal totalQuantity = lines.stream().map(MaterialItem::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        Comparator<MaterialItem> byPrice = Comparator.comparing(PurchaseStatistics::unitPriceExcludingVat);
        return new ProductOverview(product, lines.size(), totalQuantity,
                priced.stream().min(byPrice).map(PurchaseStatistics::supplierPrice).orElse(null),
                priced.stream().max(byPrice).map(PurchaseStatistics::supplierPrice).orElse(null),
                priced.stream().sorted(NEWEST_LINE_FIRST).findFirst()
                        .map(PurchaseStatistics::unitPriceExcludingVat).orElse(null),
                priced.isEmpty() ? null : totalExpense(priced),
                CanonicalNames.distinct(lines.stream().map(MaterialItem::getSupplierName).toList()));
    }

    /** Summary of usage rows; the rows must be newest first. */
    public static ProductUsageSummary summarize(List<ProductUsageRow> rows) {
        if (rows.isEmpty()) {
            return ProductUsageSummary.empty();
        }
        ProductUsageRow last = rows.get(0);
        List<BigDecimal> salePrices = rows.stream().map(ProductUsageRow::saleUnitPrice)
                .filter(Objects::nonNull).toList();
        List<ProductUsageRow> withSupplier = rows.stream()
                .filter(row -> row.supplierName() != null && row.purchaseUnitPrice() != null)
                .toList();
        Comparator<ProductUsageRow> byPrice = Comparator.comparing(ProductUsageRow::purchaseUnitPriceExcludingVat);
        return new ProductUsageSummary(
                rows.size(),
                rows.stream().map(ProductUsageRow::quantity).reduce(BigDecimal.ZERO, BigDecimal::add),
                costOfRows(rows),
                last.customerName(),
                last.date(),
                salePrices.stream().min(Comparator.naturalOrder()).orElse(null),
                salePrices.stream().max(Comparator.naturalOrder()).orElse(null),
                salePrices.isEmpty() ? null : salePrices.get(0),
                rows.stream().map(ProductUsageRow::purchaseUnitPriceExcludingVat).filter(Objects::nonNull)
                        .findFirst().orElse(null),
                withSupplier.stream().min(byPrice).map(PurchaseStatistics::supplierPrice).orElse(null),
                withSupplier.stream().max(byPrice).map(PurchaseStatistics::supplierPrice).orElse(null));
    }

    /** One entry per supplier (spelling variants merged), cheapest average first. */
    public static List<SupplierPriceComparison> compareSuppliers(List<ProductUsageRow> rows) {
        Map<String, List<ProductUsageRow>> bySupplier = new LinkedHashMap<>();
        rows.stream()
                .filter(row -> row.supplierName() != null && row.purchaseUnitPrice() != null)
                .forEach(row -> bySupplier.computeIfAbsent(MetinKarsilastirici.normalize(row.supplierName()),
                        key -> new java.util.ArrayList<>()).add(row));
        return bySupplier.values().stream()
                .map(PurchaseStatistics::comparison)
                .sorted(Comparator.comparing(SupplierPriceComparison::averagePrice)
                        .thenComparing(SupplierPriceComparison::supplierName))
                .toList();
    }

    private static SupplierPriceComparison comparison(List<ProductUsageRow> rows) {
        List<BigDecimal> prices = rows.stream().map(ProductUsageRow::purchaseUnitPriceExcludingVat).toList();
        BigDecimal sum = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(prices.size()), MONEY_SCALE, RoundingMode.HALF_UP);
        Optional<LocalDate> lastDate = rows.stream().map(ProductUsageRow::date).filter(Objects::nonNull)
                .max(Comparator.naturalOrder());
        return new SupplierPriceComparison(rows.get(0).supplierName(), rows.size(),
                prices.stream().min(Comparator.naturalOrder()).orElseThrow(),
                prices.stream().max(Comparator.naturalOrder()).orElseThrow(), average, lastDate.orElse(null));
    }

    /** quantity × VAT-exclusive purchase price, rounded once per VAT rate like every job cost. */
    private static BigDecimal totalExpense(List<MaterialItem> pricedLines) {
        return KdvHesaplayici.calculate(JobSummaryCalculator.purchaseLines(pricedLines)).excludingVat();
    }

    private static BigDecimal costOfRows(List<ProductUsageRow> rows) {
        List<KdvHesaplayici.Line> lines = rows.stream()
                .filter(row -> row.purchaseUnitPrice() != null)
                .map(row -> new KdvHesaplayici.Line(row.quantity().multiply(row.purchaseUnitPrice()),
                        row.purchaseVatRate(), row.purchaseVatIncluded()))
                .toList();
        return lines.isEmpty() ? null : KdvHesaplayici.calculate(lines).excludingVat();
    }

    private static SupplierPrice supplierPrice(MaterialItem item) {
        return new SupplierPrice(unitPriceExcludingVat(item), item.getSupplierName());
    }

    private static SupplierPrice supplierPrice(ProductUsageRow row) {
        return new SupplierPrice(row.purchaseUnitPriceExcludingVat(), row.supplierName());
    }
}
