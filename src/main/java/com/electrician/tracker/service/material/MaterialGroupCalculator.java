package com.electrician.tracker.service.material;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import com.electrician.tracker.domain.material.MaterialItem;
import com.electrician.tracker.dto.material.MaterialGroup;
import com.electrician.tracker.dto.material.MaterialGroupTotal;
import com.electrician.tracker.service.common.KdvHesaplayici;
import com.electrician.tracker.service.common.MetinKarsilastirici;
import com.electrician.tracker.service.job.JobSummaryCalculator;

/**
 * Pure grouping of a job's material lines by their group name. Names that
 * differ only in case, Turkish letters or spacing ("diafon" / "DİAFON") are
 * one group shown with the spelling of its earliest saved line; a blank name
 * is the uncategorized group ({@code groupName == null}). Each group's
 * figures reuse {@link JobSummaryCalculator}'s line building and
 * {@link KdvHesaplayici}, so the total over every line equals the job's
 * material totals exactly.
 */
public class MaterialGroupCalculator {

    private static final int MONEY_SCALE = 2;
    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");
    private static final String UNCATEGORIZED_KEY = "";

    /** Groups A–Z in Turkish order, uncategorized last; lines by product name. */
    public List<MaterialGroup> group(List<MaterialItem> items) {
        return groupedLines(items).entrySet().stream()
                .map(entry -> new MaterialGroup(totalOf(entry.getKey(), entry.getValue()), entry.getValue()))
                .sorted(Comparator.comparing(MaterialGroup::total, alphabetical()))
                .toList();
    }

    /** Each group's figures, A–Z in Turkish order, uncategorized last. */
    public List<MaterialGroupTotal> totals(List<MaterialItem> items) {
        return group(items).stream().map(MaterialGroup::total).toList();
    }

    /** The figures over every line ("Toplam"); its name is {@code null}. */
    public MaterialGroupTotal total(List<MaterialItem> items) {
        return totalOf(null, items);
    }

    /** Most spent first (purchase total), uncategorized last. */
    public static List<MaterialGroupTotal> bySpending(List<MaterialGroupTotal> totals) {
        return sortedDescending(totals, MaterialGroupTotal::purchaseTotal);
    }

    /** Highest sale first, uncategorized last (when costs are hidden, so is their order). */
    public static List<MaterialGroupTotal> bySale(List<MaterialGroupTotal> totals) {
        return sortedDescending(totals, MaterialGroupTotal::saleTotal);
    }

    /** Lines by normalized group key; the key's display name is the earliest saved line's spelling. */
    private Map<String, List<MaterialItem>> groupedLines(List<MaterialItem> items) {
        Map<String, List<MaterialItem>> byKey = new LinkedHashMap<>();
        Map<String, String> displayNames = new LinkedHashMap<>();
        items.stream().sorted(Comparator.comparing(MaterialItem::getId,
                Comparator.nullsLast(Comparator.naturalOrder()))).forEach(item -> {
                    String key = MetinKarsilastirici.normalize(item.getGroupName());
                    byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
                    displayNames.putIfAbsent(key, key.isEmpty() ? null : item.getGroupName().trim());
                });
        Map<String, List<MaterialItem>> byName = new LinkedHashMap<>();
        byKey.forEach((key, lines) -> byName.put(key.equals(UNCATEGORIZED_KEY) ? null : displayNames.get(key),
                lines.stream().sorted(byProductName()).toList()));
        return byName;
    }

    private static MaterialGroupTotal totalOf(String groupName, List<MaterialItem> lines) {
        BigDecimal purchase = KdvHesaplayici.calculate(JobSummaryCalculator.purchaseLines(lines)).excludingVat();
        BigDecimal sale = KdvHesaplayici.calculate(JobSummaryCalculator.materialSaleLines(lines)).excludingVat();
        return new MaterialGroupTotal(groupName, lines.size(), scale(purchase), scale(sale),
                scale(sale.subtract(purchase)), JobSummaryCalculator.countMissingPurchasePrice(lines));
    }

    private static Comparator<MaterialItem> byProductName() {
        Collator collator = Collator.getInstance(TURKISH);
        return Comparator.<MaterialItem, String>comparing(item -> item.getProduct().getDisplayName(), collator)
                .thenComparing(MaterialItem::getItemDate, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static Comparator<MaterialGroupTotal> alphabetical() {
        Collator collator = Collator.getInstance(TURKISH);
        return uncategorizedLast().thenComparing(total -> total.isUncategorized() ? "" : total.groupName(), collator);
    }

    private static List<MaterialGroupTotal> sortedDescending(List<MaterialGroupTotal> totals,
            Function<MaterialGroupTotal, BigDecimal> amount) {
        return totals.stream()
                .sorted(uncategorizedLast().thenComparing(amount,
                        Comparator.nullsLast(Comparator.<BigDecimal>reverseOrder())))
                .toList();
    }

    private static Comparator<MaterialGroupTotal> uncategorizedLast() {
        return Comparator.comparing(MaterialGroupTotal::isUncategorized);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
