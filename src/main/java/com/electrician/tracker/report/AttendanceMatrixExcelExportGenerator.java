package com.electrician.tracker.report;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.electrician.tracker.dto.AttendanceMatrix;
import com.electrician.tracker.service.AttendanceMath;
import com.electrician.tracker.ui.util.Bicimlendirici;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Writes the attendance calendar: one row per employee, one column per day,
 * each cell naming the job(s) worked ("½" marks a half day), plus a day
 * total column that sums day factors.
 */
@Component
public class AttendanceMatrixExcelExportGenerator {

    private static final DateTimeFormatter DAY_HEADER = Bicimlendirici.SHORT_DATE;
    private static final String HALF_DAY_MARK = " (½)";
    private static final String CELL_SEPARATOR = ", ";

    public void export(AttendanceMatrix matrix, Path outputFile) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(outputFile.toFile())) {
            Sheet sheet = workbook.createSheet("Puantaj");
            writeHeader(sheet, matrix.dates());
            int rowIndex = 1;
            for (AttendanceMatrix.Row row : matrix.rows()) {
                writeEmployeeRow(sheet.createRow(rowIndex++), row, matrix.dates());
            }
            ExcelFormatting.header(sheet);
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(matrix.dates().size() + 1);
            workbook.write(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeHeader(Sheet sheet, List<LocalDate> dates) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Personel");
        for (int i = 0; i < dates.size(); i++) {
            header.createCell(i + 1).setCellValue(dates.get(i).format(DAY_HEADER));
        }
        header.createCell(dates.size() + 1).setCellValue("Toplam Gün");
    }

    private void writeEmployeeRow(Row row, AttendanceMatrix.Row employeeRow, List<LocalDate> dates) {
        row.createCell(0).setCellValue(employeeRow.employeeName());
        BigDecimal totalDays = BigDecimal.ZERO;
        for (int i = 0; i < dates.size(); i++) {
            List<AttendanceMatrix.Cell> cells = employeeRow.cells().getOrDefault(dates.get(i), List.of());
            if (!cells.isEmpty()) {
                row.createCell(i + 1).setCellValue(cellText(cells));
            }
            for (AttendanceMatrix.Cell cell : cells) {
                totalDays = totalDays.add(cell.dayFactor());
            }
        }
        row.createCell(dates.size() + 1).setCellValue(totalDays.doubleValue());
    }

    private String cellText(List<AttendanceMatrix.Cell> cells) {
        return cells.stream()
                .map(cell -> cell.jobShortName()
                        + (cell.dayFactor().compareTo(AttendanceMath.HALF_DAY) == 0 ? HALF_DAY_MARK : ""))
                .collect(Collectors.joining(CELL_SEPARATOR));
    }
}
