package com.electrician.tracker.report;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.electrician.tracker.domain.Company;
import com.electrician.tracker.dto.QuoteDraft;
import com.electrician.tracker.dto.QuoteLine;
import com.electrician.tracker.dto.QuoteTotals;
import com.electrician.tracker.dto.QuoteView;
import com.electrician.tracker.service.QuoteCalculator;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EnumLabels;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

/**
 * The customer's "TEKLİF FORMU" on one A4 page: letterhead, page number,
 * customer and quote blocks, item table, totals, notes and the two signature
 * boxes. Headings and the table header use the company's brand colour; the
 * item font shrinks with the number of lines (see {@link QuoteTypography}).
 * Product names and brands, line totals and the grand total are bold black.
 * Fonts are embedded so Turkish letters print correctly.
 */
@Component
public class QuotePdfGenerator {

    private static final float TITLE_SIZE = 14;
    private static final float BLOCK_TITLE_SIZE = 9;
    private static final float INFO_SIZE = 8;
    private static final float NOTES_SIZE = 8;
    private static final float GRAND_TOTAL_SIZE = 11;
    private static final float MARGIN = 32;
    private static final float TOP_MARGIN = 28;
    private static final float GAP = 5;
    private static final float INFO_PADDING = 1.2f;
    private static final float TOTAL_PADDING = 2;
    private static final float[] INFO_WIDTHS = { 3, 2 };
    private static final float[] INFO_BLOCK_WIDTHS = { 1, 3 };
    private static final float[] ITEM_WIDTHS = { 5, 12, 37, 9, 8, 14, 15 };
    private static final float[] TOTALS_WIDTHS = { 3, 2 };
    private static final float TOTALS_WIDTH_PERCENT = 42;
    private static final float SIGNATURE_HEIGHT = 48;
    private static final String[] ITEM_HEADER_KEYS = {
            "quote.column.lineNo", "quote.column.brand", "quote.column.product", "quote.column.quantity",
            "quote.column.unit", "quote.column.unitPrice", "quote.column.total"
    };
    private static final int FIRST_NUMERIC_HEADER = 3;
    private static final int UNIT_HEADER = 4;

    public void generate(QuoteView quote, Company company, Path outputFile) {
        PdfStyle style = new PdfStyle(company);
        Document document = new Document(PageSize.A4, MARGIN, MARGIN, TOP_MARGIN, MARGIN);
        try (FileOutputStream out = new FileOutputStream(outputFile.toFile())) {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PageNumberStamp());
            document.open();
            PdfCompanyHeader.add(document, company, style);
            addTitle(document, style);
            addInfoBlocks(document, quote.draft(), style);
            document.add(new Paragraph(DialogUtil.message("quote.pdf.intro"), PdfStyle.regular(INFO_SIZE)));
            addItems(document, quote.draft().lines(), style);
            addTotals(document, quote.totals());
            addNotes(document, quote.draft().notes(), style);
            addSignatures(document, quote, style);
            document.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not generate quote PDF", e);
        }
    }

    private void addTitle(Document document, PdfStyle style) throws DocumentException {
        Paragraph title = style.headingParagraph(DialogUtil.message("quote.pdf.title"), TITLE_SIZE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(GAP);
        document.add(title);
    }

    private void addInfoBlocks(Document document, QuoteDraft draft, PdfStyle style) throws DocumentException {
        PdfPTable customer = infoBlock(style, "quote.pdf.customerBlock", List.of(
                row("quote.field.companyName", draft.companyName()),
                row("quote.field.address", draft.address()),
                row("quote.field.contactPerson", draft.contactPerson()),
                row("quote.field.email", draft.email()),
                row("quote.field.subject", draft.subject())));
        PdfPTable quote = infoBlock(style, "quote.pdf.quoteBlock", List.of(
                row("quote.field.date", Bicimlendirici.date(draft.quoteDate())),
                row("quote.field.number", draft.quoteNo()),
                row("quote.field.phone", draft.phone()),
                row("quote.field.fax", draft.fax())));
        PdfPTable blocks = new PdfPTable(INFO_WIDTHS);
        blocks.setWidthPercentage(100);
        blocks.setSpacingAfter(GAP);
        blocks.addCell(style.frame(new PdfPCell(customer)));
        blocks.addCell(style.frame(new PdfPCell(quote)));
        document.add(blocks);
    }

    private static String[] row(String labelKey, String value) {
        return new String[] { DialogUtil.message(labelKey), Objects.requireNonNullElse(value, "") };
    }

    /** Block title in the brand colour, then label/value rows. */
    private PdfPTable infoBlock(PdfStyle style, String titleKey, List<String[]> rows) {
        PdfPTable table = new PdfPTable(INFO_BLOCK_WIDTHS);
        table.setWidthPercentage(100);
        PdfPCell title = PdfStyle.plainCell(DialogUtil.message(titleKey), style.heading(BLOCK_TITLE_SIZE),
                Element.ALIGN_LEFT, INFO_PADDING);
        title.setColspan(INFO_BLOCK_WIDTHS.length);
        table.addCell(title);
        for (String[] row : rows) {
            table.addCell(PdfStyle.plainCell(row[0], PdfStyle.bold(INFO_SIZE), Element.ALIGN_LEFT, INFO_PADDING));
            table.addCell(PdfStyle.plainCell(row[1], PdfStyle.regular(INFO_SIZE), Element.ALIGN_LEFT, INFO_PADDING));
        }
        return table;
    }

    /** Sıra no first; product and brand bold; numbers right-aligned; line totals bold; zebra rows. */
    private void addItems(Document document, List<QuoteLine> lines, PdfStyle style) throws DocumentException {
        float size = QuoteTypography.itemFontSize(lines.size());
        float padding = QuoteTypography.cellPadding(size);
        Font bold = PdfStyle.bold(size);
        Font regular = PdfStyle.regular(size);
        PdfPTable table = new PdfPTable(ITEM_WIDTHS);
        table.setWidthPercentage(100);
        table.setSpacingBefore(GAP);
        table.setHeaderRows(1);
        for (int i = 0; i < ITEM_HEADER_KEYS.length; i++) {
            int alignment = i >= FIRST_NUMERIC_HEADER && i != UNIT_HEADER ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT;
            table.addCell(style.headerCell(DialogUtil.message(ITEM_HEADER_KEYS[i]), size, alignment, padding));
        }
        int rowIndex = 0;
        for (QuoteLine line : lines) {
            table.addCell(PdfStyle.bodyCell(String.valueOf(line.lineNo()), regular, Element.ALIGN_RIGHT, rowIndex,
                    padding));
            table.addCell(PdfStyle.bodyCell(line.brand(), bold, Element.ALIGN_LEFT, rowIndex, padding));
            table.addCell(PdfStyle.bodyCell(productText(line), bold, Element.ALIGN_LEFT, rowIndex, padding));
            table.addCell(PdfStyle.bodyCell(Bicimlendirici.quantity(line.quantity()), regular, Element.ALIGN_RIGHT,
                    rowIndex, padding));
            table.addCell(PdfStyle.bodyCell(EnumLabels.label(line.unit()), regular, Element.ALIGN_LEFT, rowIndex,
                    padding));
            table.addCell(PdfStyle.bodyCell(Bicimlendirici.moneyTl(line.unitPrice()), regular, Element.ALIGN_RIGHT,
                    rowIndex, padding));
            table.addCell(PdfStyle.bodyCell(Bicimlendirici.moneyTl(
                    QuoteCalculator.lineTotal(line.quantity(), line.unitPrice())), bold, Element.ALIGN_RIGHT,
                    rowIndex, padding));
            rowIndex++;
        }
        document.add(table);
    }

    private static String productText(QuoteLine line) {
        if (line.description() == null || line.description().isBlank()) {
            return line.productName();
        }
        return line.productName() + " (" + line.description() + ")";
    }

    /** TOPLAM … KDV in regular black, GENEL TOPLAM largest and bold on a grey band under a thin line. */
    private void addTotals(Document document, QuoteTotals totals) throws DocumentException {
        PdfPTable table = new PdfPTable(TOTALS_WIDTHS);
        table.setWidthPercentage(TOTALS_WIDTH_PERCENT);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setSpacingBefore(GAP);
        addTotalRow(table, DialogUtil.message("quote.total.items"), totals.itemsTotal());
        addTotalRow(table, DialogUtil.message("quote.total.discount"), totals.discount());
        addTotalRow(table, DialogUtil.message("quote.total.labor"), totals.labor());
        addTotalRow(table, DialogUtil.message("quote.total.subtotal"), totals.subtotal());
        String vatCaption = totals.vatRate() == null ? DialogUtil.message("quote.total.noVat")
                : DialogUtil.message("quote.total.vat", String.valueOf(totals.vatRate()));
        addTotalRow(table, vatCaption, totals.vatAmount());
        Font grand = PdfStyle.bold(GRAND_TOTAL_SIZE);
        table.addCell(PdfStyle.grandTotalCell(DialogUtil.message("quote.total.grand"), grand, Element.ALIGN_LEFT,
                TOTAL_PADDING * 2));
        table.addCell(PdfStyle.grandTotalCell(Bicimlendirici.moneyTl(totals.grandTotal()), grand, Element.ALIGN_RIGHT,
                TOTAL_PADDING * 2));
        document.add(table);
    }

    private void addTotalRow(PdfPTable table, String caption, BigDecimal amount) {
        table.addCell(PdfStyle.plainCell(caption, PdfStyle.regular(INFO_SIZE), Element.ALIGN_LEFT, TOTAL_PADDING));
        table.addCell(PdfStyle.plainCell(Bicimlendirici.moneyTl(amount), PdfStyle.regular(INFO_SIZE),
                Element.ALIGN_RIGHT, TOTAL_PADDING));
    }

    private void addNotes(Document document, String notes, PdfStyle style) throws DocumentException {
        if (notes == null || notes.isBlank()) {
            return;
        }
        Paragraph title = style.headingParagraph(DialogUtil.message("quote.field.notes"), BLOCK_TITLE_SIZE);
        title.setSpacingBefore(GAP);
        document.add(title);
        document.add(new Paragraph(notes, PdfStyle.regular(NOTES_SIZE)));
    }

    private void addSignatures(Document document, QuoteView quote, PdfStyle style) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(GAP * 2);
        table.setKeepTogether(true);
        table.addCell(signatureCell(style, DialogUtil.message("quote.signature.preparedBy"),
                Objects.requireNonNullElse(quote.preparedByName(), ""),
                Objects.requireNonNullElse(quote.preparedByTitle(), "")));
        table.addCell(signatureCell(style, DialogUtil.message("quote.signature.approval"), "", ""));
        document.add(table);
    }

    private PdfPCell signatureCell(PdfStyle style, String caption, String name, String title) {
        PdfPCell cell = new PdfPCell();
        cell.setMinimumHeight(SIGNATURE_HEIGHT);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(PdfStyle.RULE);
        cell.addElement(new Paragraph(caption, style.heading(INFO_SIZE)));
        cell.addElement(new Paragraph(name, PdfStyle.bold(INFO_SIZE)));
        cell.addElement(new Paragraph(title, PdfStyle.regular(INFO_SIZE)));
        return cell;
    }
}
