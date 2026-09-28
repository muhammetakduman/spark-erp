package com.electrician.tracker.report;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.electrician.tracker.domain.Company;
import com.electrician.tracker.domain.DailyJobStatus;
import com.electrician.tracker.dto.DailyJobCard;
import com.electrician.tracker.dto.DayPlan;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.DialogUtil;
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
 * "Günlük Program": one plain page in large print to carry in the pocket —
 * the day, and per job the time, title, customer, address, phone and who
 * goes, with an empty box to tick. Cancelled entries and those moved to
 * another day are left out.
 */
@Component
public class DailyPlanPdfGenerator {

    private static final Set<DailyJobStatus> PRINTED =
            Set.of(DailyJobStatus.PLANNED, DailyJobStatus.COMPLETED, DailyJobStatus.NOT_VISITED);
    private static final float TITLE_SIZE = 18;
    private static final float COMPANY_SIZE = 10;
    private static final float TEXT_SIZE = 12;
    private static final float SMALL_SIZE = 10;
    private static final float MARGIN = 30;
    private static final float GAP = 8;
    private static final float PADDING = 5;
    private static final float[] WIDTHS = { 9, 40, 30, 21 };
    private static final String LINE_BREAK = "\n";
    private static final String NAME_SEPARATOR = ", ";

    public void generate(DayPlan plan, Company company, Path outputFile) {
        PdfStyle style = new PdfStyle(company);
        Document document = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN);
        try (FileOutputStream out = new FileOutputStream(outputFile.toFile())) {
            PdfWriter.getInstance(document, out);
            document.open();
            if (company != null && company.getName() != null) {
                document.add(new Paragraph(company.getName(), PdfStyle.muted(COMPANY_SIZE)));
            }
            Paragraph title = style.headingParagraph(DialogUtil.message("dailyPdf.title",
                    Bicimlendirici.dateWithDay(plan.date())), TITLE_SIZE);
            title.setSpacingAfter(GAP);
            document.add(title);
            List<DailyJobCard> printed = plan.cards().stream().filter(card -> PRINTED.contains(card.status())).toList();
            if (printed.isEmpty()) {
                document.add(new Paragraph(DialogUtil.message("dailyPdf.empty"), PdfStyle.regular(TEXT_SIZE)));
            } else {
                document.add(table(printed, style));
            }
            document.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not generate daily program PDF", e);
        }
    }

    private PdfPTable table(List<DailyJobCard> cards, PdfStyle style) {
        PdfPTable table = new PdfPTable(WIDTHS);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String key : List.of("dailyPdf.column.time", "dailyPdf.column.job", "dailyPdf.column.place",
                "dailyPdf.column.team")) {
            table.addCell(style.headerCell(DialogUtil.message(key), TEXT_SIZE, Element.ALIGN_LEFT, PADDING));
        }
        int rowIndex = 0;
        for (DailyJobCard card : cards) {
            table.addCell(PdfStyle.bodyCell(card.timeOfDay() == null ? "" : card.timeOfDay(), PdfStyle.bold(TEXT_SIZE),
                    Element.ALIGN_LEFT, rowIndex, PADDING));
            table.addCell(jobCell(card, rowIndex));
            table.addCell(PdfStyle.bodyCell(placeText(card), PdfStyle.regular(SMALL_SIZE), Element.ALIGN_LEFT,
                    rowIndex, PADDING));
            table.addCell(PdfStyle.bodyCell(String.join(NAME_SEPARATOR, card.employeeNames()),
                    PdfStyle.regular(SMALL_SIZE), Element.ALIGN_LEFT, rowIndex, PADDING));
            rowIndex++;
        }
        return table;
    }

    /** "☐ ACİL Priz arızası" with the customer below; the box is ticked by hand. */
    private PdfPCell jobCell(DailyJobCard card, int rowIndex) {
        PdfPCell cell = PdfStyle.bodyCell("", PdfStyle.bold(TEXT_SIZE), Element.ALIGN_LEFT, rowIndex, PADDING);
        Phrase phrase = new Phrase();
        Font bold = PdfStyle.bold(TEXT_SIZE);
        if (card.isUrgent()) {
            phrase.add(new Phrase(DialogUtil.message("dailyPdf.urgent") + " ", bold));
        }
        phrase.add(new Phrase(card.title(), bold));
        if (card.customerName() != null) {
            phrase.add(new Phrase(LINE_BREAK + card.customerName(), PdfStyle.regular(SMALL_SIZE)));
        }
        if (card.note() != null) {
            phrase.add(new Phrase(LINE_BREAK + card.note(), PdfStyle.muted(SMALL_SIZE)));
        }
        cell.setPhrase(phrase);
        return cell;
    }

    private static String placeText(DailyJobCard card) {
        List<String> lines = new ArrayList<>();
        if (card.address() != null) {
            lines.add(card.address());
        }
        if (card.phone() != null) {
            lines.add(DialogUtil.message("dailyPdf.phone", card.phone()));
        }
        return String.join(LINE_BREAK, lines);
    }
}
