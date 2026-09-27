package com.electrician.tracker.report;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;

import com.electrician.tracker.domain.Product;
import com.electrician.tracker.dto.ProductUsageReport;
import com.electrician.tracker.dto.ProductUsageRow;
import com.electrician.tracker.ui.util.Bicimlendirici;
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
 */
@Component
public class ProductUsageExcelExportGenerator {

    private static final String[] HEADERS = {
            "Tarih", "Müşteri", "İş", "Tip", "Miktar", "Birim", "Alış Fiyatı", "Alış KDV", "Tedarikçi",
            "Satış Fiyatı", "Satış KDV", "Toplam"
    };
    private static final int MAX_SHEET_NAME_LENGTH = 31;

    public void export(Product product, ProductUsageReport report, Path outputFile) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(outputFile.toFile())) {
            Sheet sheet = workbook.createSheet(sheetName(product.getName()));
            writeHeader(sheet);
            int rowIndex = 1;
            for (ProductUsageRow usage : report.rows()) {
                writeRow(sheet.createRow(rowIndex++), product, usage);
            }
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeHeader(Sheet sheet) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            row.createCell(i).setCellValue(HEADERS[i]);
        }
    }

    private void writeRow(Row row, Product product, ProductUsageRow usage) {
        row.createCell(0).setCellValue(Bicimlendirici.date(usage.date()));
        row.createCell(1).setCellValue(usage.customerName());
        row.createCell(2).setCellValue(usage.jobName() == null ? "" : usage.jobName());
        row.createCell(3).setCellValue(EnumLabels.label(usage.jobType()));
        setNumber(row, 4, usage.quantity());
        row.createCell(5).setCellValue(EnumLabels.label(product.getUnit()));
        setNumber(row, 6, usage.purchaseUnitPrice());
        row.createCell(7).setCellValue(VatLabels.describe(usage.purchaseVatRate(), usage.purchaseVatIncluded()));
        row.createCell(8).setCellValue(usage.supplierName() == null ? "" : usage.supplierName());
        setNumber(row, 9, usage.saleUnitPrice());
        row.createCell(10).setCellValue(VatLabels.describe(usage.vatRate(), usage.vatIncluded()));
        setNumber(row, 11, usage.saleTotal());
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
