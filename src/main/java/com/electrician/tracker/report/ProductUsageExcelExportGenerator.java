package com.electrician.tracker.report;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.electrician.tracker.domain.Product;
import com.electrician.tracker.dto.ProductUsageReport;
import com.electrician.tracker.dto.ProductUsageRow;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.VatLabels;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Writes a product's usage history: one row per time it was given, newest
 * first, with amounts as numeric cells so they can be summed in Excel.
 * Purchases carry currency, foreign amount, rate and lira value in separate
 * columns. Without {@code includePurchase} the purchase columns are not
 * created at all.
 */
@Component
public class ProductUsageExcelExportGenerator {

    private static final List<String> BASE_HEADER_KEYS = List.of("excel.column.date", "excel.column.customer",
            "excel.column.job", "excel.column.type", "excel.column.quantity", "excel.column.unit");
    private static final List<String> PURCHASE_HEADER_KEYS = List.of("excel.column.currency",
            "excel.column.foreignAmount", "excel.column.exchangeRate", "excel.column.purchaseTl",
            "excel.column.purchaseVat", "excel.column.supplier");
    private static final List<String> SALE_HEADER_KEYS = List.of("excel.column.salePrice", "excel.column.saleVat",
            "excel.column.total");
    private static final int MAX_SHEET_NAME_LENGTH = 31;

    public void export(Product product, ProductUsageReport report, Path outputFile, boolean includePurchase) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(outputFile.toFile())) {
            Sheet sheet = workbook.createSheet(sheetName(product.getDisplayName()));
            List<String> headers = headers(includePurchase);
            writeHeader(sheet, headers);
            int rowIndex = 1;
            for (ProductUsageRow usage : report.rows()) {
                writeRow(sheet.createRow(rowIndex++), product, usage, includePurchase);
            }
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> headers(boolean includePurchase) {
        List<String> keys = new ArrayList<>(BASE_HEADER_KEYS);
        if (includePurchase) {
            keys.addAll(PURCHASE_HEADER_KEYS);
        }
        keys.addAll(SALE_HEADER_KEYS);
        return keys.stream().map(DialogUtil::message).toList();
    }

    private void writeHeader(Sheet sheet, List<String> headers) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            row.createCell(i).setCellValue(headers.get(i));
        }
    }

    private void writeRow(Row row, Product product, ProductUsageRow usage, boolean includePurchase) {
        row.createCell(0).setCellValue(Bicimlendirici.date(usage.date()));
        row.createCell(1).setCellValue(usage.customerName());
        row.createCell(2).setCellValue(usage.jobName() == null ? "" : usage.jobName());
        row.createCell(3).setCellValue(EnumLabels.label(usage.jobType()));
        setNumber(row, 4, usage.quantity());
        row.createCell(5).setCellValue(EnumLabels.label(product.getUnit()));
        int column = BASE_HEADER_KEYS.size();
        if (includePurchase) {
            column = writePurchase(row, column, usage);
        }
        setNumber(row, column++, usage.saleUnitPrice());
        row.createCell(column++).setCellValue(VatLabels.describe(usage.vatRate(), usage.vatIncluded()));
        setNumber(row, column, usage.saleTotal());
    }

    /** Para birimi | Döviz tutarı | Kur | TL karşılığı | Alış KDV | Tedarikçi. */
    private int writePurchase(Row row, int firstColumn, ProductUsageRow usage) {
        int column = firstColumn;
        boolean priced = usage.purchaseUnitPrice() != null;
        boolean foreign = usage.isForeignCurrencyPurchase();
        row.createCell(column++).setCellValue(priced && usage.purchaseCurrency() != null
                ? usage.purchaseCurrency().name() : "");
        setNumber(row, column++, foreign ? usage.purchaseOriginalUnitPrice() : null);
        setNumber(row, column++, foreign ? usage.purchaseExchangeRate() : null);
        setNumber(row, column++, usage.purchaseUnitPrice());
        row.createCell(column++).setCellValue(priced
                ? VatLabels.describe(usage.purchaseVatRate(), usage.purchaseVatIncluded()) : "");
        row.createCell(column++).setCellValue(usage.supplierName() == null ? "" : usage.supplierName());
        return column;
    }

    private void setNumber(Row row, int column, BigDecimal value) {
        Cell cell = row.createCell(column);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value.doubleValue());
        }
    }

    /** Excel forbids some characters and more than 31 characters in sheet names. */
    private String sheetName(String productName) {
        String safe = productName.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        return safe.length() > MAX_SHEET_NAME_LENGTH ? safe.substring(0, MAX_SHEET_NAME_LENGTH) : safe;
    }
}
