package com.electrician.tracker.report.excel;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.electrician.tracker.domain.job.Job;
import com.electrician.tracker.domain.material.MaterialItem;
import com.electrician.tracker.dto.job.JobSummary;
import com.electrician.tracker.dto.material.MaterialGroup;
import com.electrician.tracker.dto.material.MaterialGroupTotal;
import com.electrician.tracker.dto.common.VatBreakdown;
import com.electrician.tracker.service.material.MaterialGroupCalculator;
import com.electrician.tracker.ui.util.format.Bicimlendirici;
import com.electrician.tracker.ui.util.dialog.DialogUtil;
import com.electrician.tracker.ui.util.format.EnumLabels;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Writes one row per job (site or service) to the "İşler" sheet, for the
 * "tarih aralığına göre Excel dışa aktarım" requirement. With
 * {@code includeFinancials} the wage, cost, profit, collected and remaining
 * columns are added, and a "Malzemeler" sheet lists every material line with
 * its group, purchase currency, foreign amount, rate and lira value, sorted
 * by job and group with a subtotal row after each group; without it none of
 * them is created.
 */
@Component
public class JobRangeExcelExportGenerator {
    private static final String[] BASE_HEADERS = {
            "Tarih", "Müşteri", "İş Adı", "Tip", "Durum", "Satış (KDV'siz)", "KDV Oranı", "KDV Tutarı",
            "Genel Toplam (KDV'li)", "Puantaj Günü"
    };
    private static final String[] FINANCIAL_HEADERS = {
            "Yevmiye Toplamı", "Maliyet (KDV'siz)", "Kâr", "Tahsil Edilen", "Kalan"
    };
    /** Sale excl. VAT, VAT amount, grand total; then wages, cost, profit, collected, remaining. */
    private static final int[] JOB_MONEY_COLUMNS = { 5, 7, 8, 10, 11, 12, 13, 14 };
    /**
     * Foreign amount, lira purchase price, sale price, then the VAT-excl. purchase
     * and sale amounts (the exchange rate keeps its own decimals).
     */
    private static final int[] MATERIAL_MONEY_COLUMNS = { 8, 10, 12, 13, 14 };
    private static final int GROUP_COLUMN = 3;
    private static final int PURCHASE_AMOUNT_COLUMN = 13;
    private static final List<String> MATERIAL_HEADER_KEYS = List.of("excel.column.date", "excel.column.customer",
            "excel.column.job", "excel.column.group", "excel.column.product", "excel.column.quantity",
            "excel.column.unit", "excel.column.currency", "excel.column.foreignAmount", "excel.column.exchangeRate",
            "excel.column.purchaseTl", "excel.column.supplier", "excel.column.salePrice",
            "excel.column.purchaseAmount", "excel.column.saleAmount");

    private final MaterialGroupCalculator groupCalculator = new MaterialGroupCalculator();

    /** {@code groupsByJob}: each job's material lines by group, in print order. */
    public void export(List<Job> jobs, Map<Long, JobSummary> summaries, Map<Long, List<MaterialGroup>> groupsByJob,
            Path outputFile, boolean includeFinancials) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(outputFile.toFile())) {
            Sheet sheet = workbook.createSheet("İşler");
            String[] headers = includeFinancials ? concat(BASE_HEADERS, FINANCIAL_HEADERS) : BASE_HEADERS;
            writeHeaderRow(sheet, headers);
            int rowIndex = 1;
            for (Job job : jobs) {
                JobSummary summary = summaries.get(job.getId());
                Row row = writeJobRow(sheet, rowIndex++, job, summary);
                if (includeFinancials) {
                    writeFinancials(row, summary);
                }
            }
            ExcelFormatting.moneyColumns(sheet, JOB_MONEY_COLUMNS);
            autoSize(sheet, headers.length);
            if (includeFinancials) {
                writeMaterialSheet(workbook, jobs, groupsByJob);
            }
            workbook.write(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeMaterialSheet(XSSFWorkbook workbook, List<Job> jobs, Map<Long, List<MaterialGroup>> groupsByJob) {
        Sheet sheet = workbook.createSheet(DialogUtil.message("excel.sheet.materials"));
        String[] headers = MATERIAL_HEADER_KEYS.stream().map(DialogUtil::message).toArray(String[]::new);
        writeHeaderRow(sheet, headers);
        List<Integer> subtotalRows = new ArrayList<>();
        int rowIndex = 1;
        for (Job job : jobs) {
            for (MaterialGroup group : groupsByJob.getOrDefault(job.getId(), List.of())) {
                String groupName = groupLabel(group.total());
                for (MaterialItem item : group.lines()) {
                    writeMaterialRow(sheet.createRow(rowIndex++), job, groupName, item);
                }
                subtotalRows.add(rowIndex);
                writeSubtotalRow(sheet.createRow(rowIndex++), groupName, group.total());
            }
        }
        ExcelFormatting.moneyColumns(sheet, MATERIAL_MONEY_COLUMNS);
        ExcelFormatting.boldRows(sheet, subtotalRows, MATERIAL_MONEY_COLUMNS);
        autoSize(sheet, headers.length);
    }

    private static String groupLabel(MaterialGroupTotal total) {
        return total.isUncategorized() ? DialogUtil.message("material.group.uncategorized") : total.groupName();
    }

    /** "Ara toplam – Diafon" under the group column, its purchase and sale totals (VAT excl.) at the end. */
    private void writeSubtotalRow(Row row, String groupName, MaterialGroupTotal total) {
        setCell(row, GROUP_COLUMN, DialogUtil.message("excel.groupSubtotal", groupName));
        setCell(row, PURCHASE_AMOUNT_COLUMN, total.purchaseTotal());
        setCell(row, PURCHASE_AMOUNT_COLUMN + 1, total.saleTotal());
    }

    /** … | Grup | Ürün | … | Para birimi | Döviz tutarı | Kur | TL karşılığı | … | Alış / Satış tutarı (KDV'siz) */
    private void writeMaterialRow(Row row, Job job, String groupName, MaterialItem item) {
        boolean foreign = item.isForeignCurrencyPurchase();
        MaterialGroupTotal line = groupCalculator.total(List.of(item));
        int column = 0;
        setCell(row, column++, item.getItemDate() == null ? "" : Bicimlendirici.date(item.getItemDate()));
        setCell(row, column++, job.getCustomer().getName());
        setCell(row, column++, job.getName() == null ? "" : job.getName());
        setCell(row, column++, groupName);
        setCell(row, column++, item.getProduct().getDisplayName());
        setCell(row, column++, item.getQuantity());
        setCell(row, column++, EnumLabels.label(item.getProduct().getUnit()));
        setCell(row, column++, item.getPurchaseUnitPrice() == null || item.getPurchaseCurrency() == null ? ""
                : item.getPurchaseCurrency().name());
        setCell(row, column++, foreign ? item.getPurchaseUnitPrice() : null);
        setCell(row, column++, foreign ? item.getPurchaseExchangeRate() : null);
        setCell(row, column++, item.getPurchaseUnitPriceTl());
        setCell(row, column++, item.getSupplierName() == null ? "" : item.getSupplierName());
        setCell(row, column++, item.getSaleUnitPrice());
        setCell(row, column++, item.getPurchaseUnitPriceTl() == null ? null : line.purchaseTotal());
        setCell(row, column, line.saleTotal());
    }

    private void writeHeaderRow(Sheet sheet, String[] headers) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            row.createCell(i).setCellValue(headers[i]);
        }
        ExcelFormatting.header(sheet);
    }

    private static void autoSize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static String[] concat(String[] first, String[] second) {
        String[] all = java.util.Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, all, first.length, second.length);
        return all;
    }

    private Row writeJobRow(Sheet sheet, int rowIndex, Job job, JobSummary summary) {
        Row row = sheet.createRow(rowIndex);
        setCell(row, 0, job.getStartDate() == null ? "" : job.getStartDate().format(Bicimlendirici.DATE));
        setCell(row, 1, job.getCustomer().getName());
        setCell(row, 2, job.getName());
        setCell(row, 3, EnumLabels.label(job.getType()));
        setCell(row, 4, EnumLabels.label(job.getStatus()));
        setCell(row, 5, summary.saleExcludingVat());
        setCell(row, 6, vatRatesText(summary.saleVat()));
        setCell(row, 7, summary.saleVatAmount());
        setCell(row, 8, summary.saleIncludingVat());
        setCell(row, 9, summary.attendanceDayCount());
        return row;
    }

    private void writeFinancials(Row row, JobSummary summary) {
        int column = BASE_HEADERS.length;
        setCell(row, column++, summary.attendanceTotal());
        setCell(row, column++, summary.costExcludingVat());
        setCell(row, column++, summary.profit());
        setCell(row, column++, summary.collectedTotal());
        setCell(row, column, summary.remaining());
    }

    private String vatRatesText(VatBreakdown vat) {
        return vat.rates().stream()
                .map(rate -> "%" + rate.rate())
                .collect(Collectors.joining(", "));
    }

    private void setCell(Row row, int column, String value) {
        row.createCell(column).setCellValue(value);
    }

    private void setCell(Row row, int column, BigDecimal value) {
        Cell cell = row.createCell(column);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value.doubleValue());
        }
    }
}
