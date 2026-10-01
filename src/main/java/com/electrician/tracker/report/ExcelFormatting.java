package com.electrician.tracker.report;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Cell formats shared by the Excel exports, applied to a filled sheet before
 * its columns are sized: a bold header row that stays visible while
 * scrolling, and money columns shown with two decimals and thousands
 * grouping (Excel uses the reader's own separators, "12.450,50" in Turkish).
 * The cells stay numeric, so they can still be summed.
 */
final class ExcelFormatting {

    private static final String MONEY_FORMAT = "#,##0.00";
    private static final int HEADER_ROW = 0;

    private ExcelFormatting() {
    }

    static void header(Sheet sheet) {
        Workbook workbook = sheet.getWorkbook();
        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(bold);
        Row header = sheet.getRow(HEADER_ROW);
        if (header != null) {
            header.forEach(cell -> cell.setCellStyle(style));
        }
        sheet.createFreezePane(0, HEADER_ROW + 1);
    }

    /** Numeric cells of the given columns, below the header; empty and text cells are left alone. */
    static void moneyColumns(Sheet sheet, int... columns) {
        Workbook workbook = sheet.getWorkbook();
        CellStyle money = workbook.createCellStyle();
        money.setDataFormat(workbook.createDataFormat().getFormat(MONEY_FORMAT));
        for (Row row : sheet) {
            if (row.getRowNum() == HEADER_ROW) {
                continue;
            }
            for (int column : columns) {
                Cell cell = row.getCell(column);
                if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                    cell.setCellStyle(money);
                }
            }
        }
    }
}
