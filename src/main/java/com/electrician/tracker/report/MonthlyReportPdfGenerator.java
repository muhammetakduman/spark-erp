package com.electrician.tracker.report;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.format.TextStyle;

import com.electrician.tracker.dto.EmployeeWageSummary;
import com.electrician.tracker.dto.MaterialSummaryLine;
import com.electrician.tracker.dto.MonthlyReport;
import com.electrician.tracker.dto.MonthlyReportRow;
import com.electrician.tracker.dto.ReportTotals;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.EnumLabels;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

/**
 * Internal monthly revenue/profit printout (landscape): summary figures, the
 * month's jobs with a total row, the material and attendance summaries.
 */
@Component
public class MonthlyReportPdfGenerator {

    private static final Font TITLE_FONT = PdfFonts.bold(16);
    private static final Font SECTION_FONT = PdfFonts.bold(12);
    private static final Font NORMAL_FONT = PdfFonts.regular(9);
    private static final Font BOLD_FONT = PdfFonts.bold(9);
    private static final String[] JOB_HEADERS = {
            "Tarih", "Müşteri", "İş", "Tip", "Malzeme", "Yevmiye*", "Ciro", "Maliyet", "Kâr", "Tahsilat"
    };
    private static final float[] JOB_WIDTHS = { 8, 16, 18, 7, 9, 9, 9, 9, 9, 10 };
    /** Keeps a section title off the top border of the table under it. */
    private static final float SECTION_TITLE_SPACING = 6;

    private final PdfFooter pdfFooter;

    public MonthlyReportPdfGenerator(PdfFooter pdfFooter) {
        this.pdfFooter = pdfFooter;
    }

    public void generate(MonthlyReport report, Path outputFile) {
        Document document = new Document(PageSize.A4.rotate());
        try (FileOutputStream out = new FileOutputStream(outputFile.toFile())) {
            pdfFooter.attachTo(PdfWriter.getInstance(document, out));
            document.open();
            addSummary(document, report);
            addJobs(document, report);
            addMaterials(document, report);
            addWages(document, report);
            document.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not generate PDF report", e);
        }
    }

    private void addSummary(Document document, MonthlyReport report) throws DocumentException {
        String monthName = report.month().getMonth().getDisplayName(TextStyle.FULL_STANDALONE, Bicimlendirici.TURKISH);
        document.add(new Paragraph("Aylık Rapor – " + monthName + " " + report.month().getYear(), TITLE_FONT));
        ReportTotals totals = report.totals();
        document.add(new Paragraph("Ciro (KDV'siz): " + Bicimlendirici.money(totals.revenue())
                + "   ·   KDV (bilgi): " + Bicimlendirici.money(totals.vatAmount()), SECTION_FONT));
        document.add(new Paragraph("Maliyet (malzeme alışı): " + Bicimlendirici.money(totals.cost()), SECTION_FONT));
        String estimated = totals.isProfitEstimated()
                ? "  (tahmini – " + totals.missingPurchasePriceCount() + " kalemde alış fiyatı yok)" : "";
        document.add(new Paragraph("Kâr: " + Bicimlendirici.money(totals.profit()) + estimated, SECTION_FONT));
        document.add(new Paragraph("Tahsil edilmeyen: " + Bicimlendirici.money(totals.uncollected()), SECTION_FONT));
        document.add(new Paragraph(" "));
    }

    private void addJobs(Document document, MonthlyReport report) throws DocumentException {
        PdfPTable table = new PdfPTable(JOB_WIDTHS);
        table.setWidthPercentage(100);
        for (String header : JOB_HEADERS) {
            table.addCell(new PdfPCell(new Phrase(header, BOLD_FONT)));
        }
        for (MonthlyReportRow row : report.rows()) {
            table.addCell(new Phrase(Bicimlendirici.date(row.date()), NORMAL_FONT));
            table.addCell(new Phrase(row.customerName(), NORMAL_FONT));
            table.addCell(new Phrase(row.jobName() == null ? "" : row.jobName(), NORMAL_FONT));
            table.addCell(new Phrase(EnumLabels.label(row.jobType()), NORMAL_FONT));
            addFigures(table, row.figures(), NORMAL_FONT);
        }
        PdfPCell totalCaption = new PdfPCell(new Phrase("TOPLAM", BOLD_FONT));
        totalCaption.setColspan(4);
        table.addCell(totalCaption);
        addFigures(table, report.totals(), BOLD_FONT);
        document.add(table);
        document.add(new Paragraph("* Yevmiye yalnızca bilgi içindir; maliyete ve kâra dahil edilmez.", NORMAL_FONT));
        document.add(new Paragraph(" "));
    }

    private void addFigures(PdfPTable table, ReportTotals figures, Font font) {
        addMoney(table, Bicimlendirici.money(figures.materialCost()), font);
        addMoney(table, Bicimlendirici.money(figures.wageTotal()), font);
        addMoney(table, Bicimlendirici.money(figures.revenue()), font);
        addMoney(table, Bicimlendirici.money(figures.cost()), font);
        addMoney(table, Bicimlendirici.money(figures.profit()), font);
        addMoney(table, figures.uncollected().signum() == 0 ? "Ödendi"
                : "Kalan " + Bicimlendirici.money(figures.uncollected()), font);
    }

    private void addMoney(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }

    private void addMaterials(Document document, MonthlyReport report) throws DocumentException {
        document.add(sectionTitle("Malzeme Özeti"));
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(70);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        for (String header : new String[] { "Ürün", "Miktar", "Toplam Alış", "Toplam Satış" }) {
            table.addCell(new PdfPCell(new Phrase(header, BOLD_FONT)));
        }
        for (MaterialSummaryLine line : report.materials()) {
            table.addCell(new Phrase(line.productName(), NORMAL_FONT));
            table.addCell(new Phrase(Bicimlendirici.quantity(line.quantity()) + " " + EnumLabels.label(line.unit()),
                    NORMAL_FONT));
            addMoney(table, Bicimlendirici.money(line.purchaseTotal()), NORMAL_FONT);
            addMoney(table, Bicimlendirici.money(line.saleTotal()), NORMAL_FONT);
        }
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addWages(Document document, MonthlyReport report) throws DocumentException {
        document.add(sectionTitle("Yevmiye Özeti"));
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        for (String header : new String[] { "Personel", "Gün", "Toplam Yevmiye" }) {
            table.addCell(new PdfPCell(new Phrase(header, BOLD_FONT)));
        }
        for (EmployeeWageSummary wage : report.wages()) {
            table.addCell(new Phrase(wage.employeeName(), NORMAL_FONT));
            addMoney(table, Bicimlendirici.days(wage.dayCount()), NORMAL_FONT);
            addMoney(table, Bicimlendirici.money(wage.totalWage()), NORMAL_FONT);
        }
        document.add(table);
    }

    private static Paragraph sectionTitle(String text) {
        Paragraph title = new Paragraph(text, SECTION_FONT);
        title.setSpacingAfter(SECTION_TITLE_SPACING);
        return title;
    }
}
